package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Chat;
import com.tustanovskyy.taxi.document.Message;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.ChatRequest;
import com.tustanovskyy.taxi.domain.request.MessageRequest;
import com.tustanovskyy.taxi.domain.response.ChatResponse;
import com.tustanovskyy.taxi.domain.response.MessageResponse;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.ChatRepository;
import com.tustanovskyy.taxi.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MongoTemplate mongoTemplate;

    @Transactional
    public ChatResponse createChat(ChatRequest request, String currentUserPhone) {
        var currentUser = userService.findByPhoneNumber(currentUserPhone);

        List<String> participantIds = new ArrayList<>(request.getParticipantIds());
        if (!participantIds.contains(currentUser.getId())) {
            participantIds.add(currentUser.getId());
        }

        // A ride can have separate chats with different partners, so the lookup must match
        // the exact pair of participants, not just the rideId.
        var existingChat = chatRepository.findActiveChatForParticipants(
                request.getRideId(), participantIds, participantIds.size(), true);
        if (existingChat.isPresent()) {
            return mapToChatResponses(List.of(existingChat.get()), currentUser.getId()).get(0);
        }

        var chat = Chat.builder()
                .rideId(request.getRideId())
                .participantIds(participantIds)
                .createdDate(LocalDateTime.now())
                .lastMessageDate(LocalDateTime.now())
                .isActive(true)
                .build();

        var savedChat = chatRepository.save(chat);
        log.info("Created chat: {}", savedChat);

        // Send system message
        sendSystemMessage(savedChat.getId(), "Chat created for ride sharing");

        return mapToChatResponses(List.of(savedChat), currentUser.getId()).get(0);
    }

    @Transactional
    public MessageResponse sendMessage(String chatId, MessageRequest request, String senderPhone) {
        var sender = userService.findByPhoneNumber(senderPhone);
        var chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ValidationException("Chat not found: " + chatId));

        // Verify sender is participant
        if (!chat.getParticipantIds().contains(sender.getId())) {
            throw new ValidationException("User is not a participant in this chat");
        }

        var message = Message.builder()
                .chatId(chatId)
                .senderId(sender.getId())
                .content(request.getContent())
                .type(request.getType())
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();

        var savedMessage = messageRepository.save(message);

        // Update chat last message date
        chat.setLastMessageDate(LocalDateTime.now());
        chatRepository.save(chat);

        var messageResponse = mapToMessageResponse(savedMessage, Map.of(sender.getId(), sender));

        // Send to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageResponse);

        log.info("Sent message: {}", savedMessage);
        return messageResponse;
    }

    public List<ChatResponse> getUserChats(String userPhone, String rideId) {
        var user = userService.findByPhoneNumber(userPhone);

        List<Chat> chats = (rideId != null && !rideId.isBlank())
                ? chatRepository.findByRideIdAndParticipantIdsContainingAndIsActive(rideId, user.getId(), true)
                : chatRepository.findByParticipantIdsContainingAndIsActive(user.getId(), true);

        return mapToChatResponses(chats, user.getId());
    }

    public List<MessageResponse> getChatMessages(String chatId, String userPhone, int page, int size) {
        var user = userService.findByPhoneNumber(userPhone);
        var chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ValidationException("Chat not found: " + chatId));

        // Verify user is participant
        if (!chat.getParticipantIds().contains(user.getId())) {
            throw new ValidationException("User is not a participant in this chat");
        }

        Pageable pageable = PageRequest.of(page, size);
        var messages = messageRepository.findByChatIdOrderByTimestampDesc(chatId, pageable);

        Set<String> senderIds = messages.stream()
                .map(Message::getSenderId)
                .filter(id -> !"system".equals(id))
                .collect(Collectors.toSet());
        Map<String, User> usersById = userService.findUsersByIds(senderIds);

        return messages.stream()
                .map(message -> mapToMessageResponse(message, usersById))
                .collect(Collectors.toList());
    }

    @Transactional
    public void markMessagesAsRead(String chatId, String userPhone) {
        var user = userService.findByPhoneNumber(userPhone);
        var unreadMessages = messageRepository.findByChatIdAndSenderIdNotAndIsReadFalse(chatId, user.getId());
        
        unreadMessages.forEach(message -> message.setRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    private void sendSystemMessage(String chatId, String content) {
        var systemMessage = Message.builder()
                .chatId(chatId)
                .senderId("system")
                .content(content)
                .type(Message.MessageType.SYSTEM)
                .timestamp(LocalDateTime.now())
                .isRead(false)
                .build();

        messageRepository.save(systemMessage);
    }

    /**
     * Maps a list of chats to responses using batched lookups (one query per data type,
     * regardless of chat count) instead of per-chat queries, to avoid N+1 query patterns
     * that made the inbox/chat list slow.
     */
    private List<ChatResponse> mapToChatResponses(List<Chat> chats, String currentUserId) {
        if (chats.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> chatIds = chats.stream().map(Chat::getId).collect(Collectors.toList());
        Set<String> participantIds = chats.stream()
                .flatMap(chat -> chat.getParticipantIds().stream())
                .collect(Collectors.toSet());

        Map<String, User> usersById = userService.findUsersByIds(participantIds);
        Map<String, Message> lastMessageByChatId = findLastMessagesByChatIds(chatIds);
        Map<String, Long> unreadCountByChatId = countUnreadByChatIds(chatIds, currentUserId);

        return chats.stream()
                .map(chat -> {
                    var participants = chat.getParticipantIds().stream()
                            .map(usersById::get)
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList());

                    var lastMessage = Optional.ofNullable(lastMessageByChatId.get(chat.getId()))
                            .map(message -> mapToMessageResponse(message, usersById))
                            .orElse(null);

                    var unreadCount = unreadCountByChatId.getOrDefault(chat.getId(), 0L).intValue();

                    return ChatResponse.builder()
                            .id(chat.getId())
                            .rideId(chat.getRideId())
                            .participants(participants)
                            .lastMessage(lastMessage)
                            .createdDate(chat.getCreatedDate())
                            .lastMessageDate(chat.getLastMessageDate())
                            .isActive(chat.isActive())
                            .unreadCount(unreadCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    private Map<String, Message> findLastMessagesByChatIds(List<String> chatIds) {
        if (chatIds.isEmpty()) {
            return Map.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("chatId").in(chatIds)),
                Aggregation.sort(Sort.Direction.DESC, "timestamp"),
                Aggregation.group("chatId").first(Aggregation.ROOT).as("doc")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "message", Document.class);

        Map<String, Message> lastMessages = new HashMap<>();
        for (Document doc : results.getMappedResults()) {
            String chatId = doc.getString("_id");
            Document messageDoc = doc.get("doc", Document.class);
            if (messageDoc != null) {
                lastMessages.put(chatId, mongoTemplate.getConverter().read(Message.class, messageDoc));
            }
        }
        return lastMessages;
    }

    private Map<String, Long> countUnreadByChatIds(List<String> chatIds, String currentUserId) {
        if (chatIds.isEmpty()) {
            return Map.of();
        }

        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("chatId").in(chatIds)
                        .and("isRead").is(false)
                        .and("senderId").ne(currentUserId)),
                Aggregation.group("chatId").count().as("count")
        );

        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "message", Document.class);

        Map<String, Long> counts = new HashMap<>();
        for (Document doc : results.getMappedResults()) {
            counts.put(doc.getString("_id"), ((Number) doc.get("count")).longValue());
        }
        return counts;
    }

    private MessageResponse mapToMessageResponse(Message message, Map<String, User> usersById) {
        var sender = "system".equals(message.getSenderId()) ? null : usersById.get(message.getSenderId());

        return MessageResponse.builder()
                .id(message.getId())
                .chatId(message.getChatId())
                .sender(sender)
                .content(message.getContent())
                .type(message.getType())
                .timestamp(message.getTimestamp())
                .isRead(message.isRead())
                .build();
    }
}
