package com.tustanovskyy.taxi.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tustanovskyy.taxi.document.Chat;
import com.tustanovskyy.taxi.document.Message;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.ChatRequest;
import com.tustanovskyy.taxi.domain.request.MessageRequest;
import com.tustanovskyy.taxi.domain.response.ChatResponse;
import com.tustanovskyy.taxi.domain.response.MessageResponse;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.ChatRepository;
import com.tustanovskyy.taxi.repository.MessageRepository;
import com.tustanovskyy.taxi.service.notification.ExpoPushService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
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
import java.util.Comparator;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private final RideService rideService;
    private final SimpMessagingTemplate messagingTemplate;
    private final MongoTemplate mongoTemplate;
    private final ObjectMapper objectMapper;
    private final ExpoPushService expoPushService;

    @Transactional
    public ChatResponse createChat(ChatRequest request, String currentUserPhone) {
        var currentUser = userService.findByPhoneNumber(currentUserPhone);

        List<String> participantIds = new ArrayList<>(request.getParticipantIds());
        if (!participantIds.contains(currentUser.getId())) {
            participantIds.add(currentUser.getId());
        }

        // The same two people always share one chat, regardless of which (possibly many, over
        // time) ride matched them together, and regardless of which side's own "start chat"
        // call happens to run first when both discover each other around the same time.
        var activeChats = chatRepository.findActiveChatsForParticipants(participantIds, participantIds.size(), true);

        Chat chat;
        if (activeChats.isEmpty()) {
            chat = chatRepository.save(Chat.builder()
                    .rideId(request.getRideId())
                    .participantIds(participantIds)
                    .createdDate(LocalDateTime.now())
                    .lastMessageDate(LocalDateTime.now())
                    .isActive(true)
                    .build());
            log.info("Created chat: {}", chat);
        } else {
            // Self-heals leftover duplicates from before chats were deduped by participant pair
            // alone (previously scoped per-ride, so the same pair could accumulate one "active"
            // chat per ride they'd ever matched on) - keep the most recently active one, retire
            // the rest so this pair converges back onto a single chat going forward.
            chat = activeChats.stream()
                    .max(Comparator.comparing(Chat::getLastMessageDate))
                    .orElseThrow();
            for (Chat duplicate : activeChats) {
                if (!duplicate.getId().equals(chat.getId())) {
                    duplicate.setActive(false);
                    chatRepository.save(duplicate);
                    log.info("Deactivated duplicate chat {} for participants {} in favor of {}",
                            duplicate.getId(), participantIds, chat.getId());
                }
            }
        }

        announceRideMatchIfNew(chat, currentUser.getId(), request.getRideId(), participantIds);

        return mapToChatResponses(List.of(chat), currentUser.getId()).get(0);
    }

    /**
     * Sends a fresh "chat created for ride sharing" system message the first time this pairing
     * is seen for the two ride IDs currently involved - a brand new chat, or an existing one
     * being reused because the same two people matched again for a different ride than last
     * time. Keyed on both ride IDs (order-independent) rather than just the caller's, so when
     * both sides' "start chat" calls land close together for the same live match, only one
     * message gets sent instead of one per side.
     */
    private void announceRideMatchIfNew(Chat chat, String currentUserId, String currentUserRideId, List<String> participantIds) {
        String otherUserId = participantIds.stream()
                .filter(id -> !id.equals(currentUserId))
                .findFirst()
                .orElse(null);
        if (otherUserId == null) {
            return;
        }

        Ride otherActiveRide = rideService.findActiveRideByUserId(otherUserId);
        String myRideId = currentUserRideId != null ? currentUserRideId : "none";
        String otherRideId = otherActiveRide != null ? otherActiveRide.getId() : "none";
        String pairingKey = myRideId.compareTo(otherRideId) <= 0
                ? myRideId + "|" + otherRideId
                : otherRideId + "|" + myRideId;

        Set<String> announcedKeys = chat.getAnnouncedPairingKeys();
        if (announcedKeys == null) {
            announcedKeys = new HashSet<>();
            chat.setAnnouncedPairingKeys(announcedKeys);
        }
        if (announcedKeys.contains(pairingKey)) {
            return;
        }

        sendSystemMessage(chat.getId(), buildChatCreatedContent(participantIds));
        announcedKeys.add(pairingKey);
        chat.setLastMessageDate(LocalDateTime.now());
        chatRepository.save(chat);
    }

    @Transactional
    public MessageResponse sendMessage(String chatId, MessageRequest request, String senderPhone) {
        var sender = userService.findByPhoneNumber(senderPhone);
        var chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ValidationException(ErrorCode.CHAT_NOT_FOUND, "Chat not found: " + chatId,
                        Map.of("chatId", chatId)));

        // Verify sender is participant
        if (!chat.getParticipantIds().contains(sender.getId())) {
            throw new ValidationException(ErrorCode.NOT_CHAT_PARTICIPANT, "User is not a participant in this chat");
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

        notifyRecipientOfMessage(chat, sender, savedMessage);

        log.info("Sent message: {}", savedMessage);
        return messageResponse;
    }

    /**
     * Push-notifies the other participant of a real, user-typed message. Only reachable from
     * here - system messages (ride matched/completed/rating) are created via the separate
     * sendSystemMessage helper below, never through this public sendMessage path - so every
     * message here is genuinely something someone typed, no type filtering needed.
     */
    private void notifyRecipientOfMessage(Chat chat, User sender, Message savedMessage) {
        String recipientId = chat.getParticipantIds().stream()
                .filter(id -> !id.equals(sender.getId()))
                .findFirst()
                .orElse(null);
        if (recipientId == null) {
            return;
        }
        User recipient = userService.findUser(recipientId);
        String senderName = (sender.getFirstName() + " " + sender.getLastName()).trim();
        String content = savedMessage.getContent() == null ? "" : savedMessage.getContent();
        String preview = content.length() > 120 ? content.substring(0, 120) + "…" : content;
        expoPushService.send(recipient.getPushToken(), senderName, preview, Map.of(
                "type", "message",
                "chatId", chat.getId(),
                "rideId", chat.getRideId() == null ? "" : chat.getRideId(),
                "partnerId", sender.getId(),
                "partnerName", senderName
        ));
    }

    public List<ChatResponse> getUserChats(String userPhone, String rideId, int page, int size) {
        var user = userService.findByPhoneNumber(userPhone);

        // A ride's own chats are naturally bounded (one per partner pairing), so only the
        // full inbox listing - which can grow to thousands of chats over a user's history -
        // needs pagination.
        List<Chat> chats = (rideId != null && !rideId.isBlank())
                ? chatRepository.findByRideIdAndParticipantIdsContainingAndIsActive(rideId, user.getId(), true)
                : chatRepository.findByParticipantIdsContainingAndIsActiveOrderByLastMessageDateDesc(
                        user.getId(), true, PageRequest.of(page, size));

        return mapToChatResponses(chats, user.getId());
    }

    public List<MessageResponse> getChatMessages(String chatId, String userPhone, int page, int size, LocalDateTime since) {
        var user = userService.findByPhoneNumber(userPhone);
        var chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new ValidationException(ErrorCode.CHAT_NOT_FOUND, "Chat not found: " + chatId,
                        Map.of("chatId", chatId)));

        // Verify user is participant
        if (!chat.getParticipantIds().contains(user.getId())) {
            throw new ValidationException(ErrorCode.NOT_CHAT_PARTICIPANT, "User is not a participant in this chat");
        }

        // The client polls this endpoint every few seconds while a chat is open. Once it has
        // an initial page, it only needs messages newer than the last one it already has -
        // `since` lets it fetch just that delta instead of re-fetching and re-serializing the
        // same page of messages on every poll.
        List<Message> messages = since != null
                ? messageRepository.findByChatIdAndTimestampAfterOrderByTimestampAsc(chatId, since)
                : messageRepository.findByChatIdOrderByTimestampDesc(chatId, PageRequest.of(page, size));

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

    /**
     * The "chat created" system message is stored as a JSON payload - each participant's own
     * name and their own ride's addresses - rather than a fixed English sentence. Each
     * participant has their OWN ride (they're matched as partners by proximity, not sharing one
     * ride record), so this looks up every participant's active ride individually rather than
     * using the single ride that happened to create the chat. Storing per-participant data lets
     * each viewer's client render it in their own app language via i18n, and pick out "my route"
     * vs "partner's route" by comparing userId to whoever's currently logged in, instead of
     * everyone seeing one hardcoded language/perspective.
     */
    private String buildChatCreatedContent(List<String> participantIds) {
        Map<String, User> usersById = userService.findUsersByIds(new HashSet<>(participantIds));

        List<Map<String, Object>> participants = participantIds.stream()
                .map(id -> {
                    User user = usersById.get(id);
                    Ride ride = rideService.findActiveRideByUserId(id);

                    Map<String, Object> participant = new LinkedHashMap<>();
                    participant.put("userId", id);
                    participant.put("name", user != null ? (user.getFirstName() + " " + user.getLastName()).trim() : null);
                    participant.put("fromAddress", ride != null && ride.getPlaceFrom() != null ? ride.getPlaceFrom().getName() : null);
                    participant.put("toAddress", ride != null && ride.getPlaceTo() != null ? ride.getPlaceTo().getName() : null);
                    return participant;
                })
                .collect(Collectors.toList());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "chatCreated");
        payload.put("participants", participants);

        return toJson(payload, "Chat created for ride sharing");
    }

    /**
     * "Ride completed" / "ride cancelled" system message payload - who did it, by name, so each
     * viewer's client can render "You marked..." vs "{{name}} marked..." by comparing the
     * actor's userId to whoever's currently logged in.
     */
    private String buildRideEventContent(String kind, String actorUserId, String fallback) {
        User actor = userService.findUsersByIds(Set.of(actorUserId)).get(actorUserId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", kind);
        payload.put("actorUserId", actorUserId);
        payload.put("actorName", actor != null ? (actor.getFirstName() + " " + actor.getLastName()).trim() : null);

        return toJson(payload, fallback);
    }

    public String buildRideCompletedContent(String actorUserId) {
        return buildRideEventContent("rideCompleted", actorUserId, "Ride completed");
    }

    public String buildRideCancelledContent(String actorUserId) {
        return buildRideEventContent("rideCancelled", actorUserId, "Ride cancelled");
    }

    /**
     * A rating is otherwise private feedback the rated user might never see in the app - posting
     * it into the shared chat as a system message (score + comment, and who left it) makes it
     * visible to both participants there, per how this app wants ride outcomes surfaced.
     */
    public String buildRatingSubmittedContent(String raterUserId, String ratedUserId, int score, String comment) {
        Map<String, User> usersById = userService.findUsersByIds(new HashSet<>(List.of(raterUserId, ratedUserId)));
        User rater = usersById.get(raterUserId);
        User rated = usersById.get(ratedUserId);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("kind", "ratingSubmitted");
        payload.put("raterUserId", raterUserId);
        payload.put("raterName", rater != null ? (rater.getFirstName() + " " + rater.getLastName()).trim() : null);
        payload.put("ratedUserId", ratedUserId);
        payload.put("ratedName", rated != null ? (rated.getFirstName() + " " + rated.getLastName()).trim() : null);
        payload.put("score", score);
        payload.put("comment", comment);

        return toJson(payload, "Rating submitted");
    }

    private String toJson(Map<String, Object> payload, String fallback) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize system message payload: {}", payload, e);
            return fallback;
        }
    }

    /**
     * Posts a system message into the existing chat between two participants - e.g. a ride
     * being completed/cancelled, or a rating being left. Unlike createChat, this never creates a
     * chat: these events only make sense once a chat/partnership already exists between them.
     */
    @Transactional
    public void sendSystemMessageToParticipants(List<String> participantIds, String content) {
        List<Chat> chats = chatRepository.findActiveChatsForParticipants(participantIds, participantIds.size(), true);
        if (chats.isEmpty()) {
            log.warn("No active chat found for participants {} - skipping system message", participantIds);
            return;
        }

        Chat chat = chats.stream().max(Comparator.comparing(Chat::getLastMessageDate)).orElseThrow();
        sendSystemMessage(chat.getId(), content);
        chat.setLastMessageDate(LocalDateTime.now());
        chatRepository.save(chat);
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
