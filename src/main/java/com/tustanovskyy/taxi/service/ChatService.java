package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Chat;
import com.tustanovskyy.taxi.document.Message;
import com.tustanovskyy.taxi.domain.request.ChatRequest;
import com.tustanovskyy.taxi.domain.request.MessageRequest;
import com.tustanovskyy.taxi.domain.response.ChatResponse;
import com.tustanovskyy.taxi.domain.response.MessageResponse;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.ChatRepository;
import com.tustanovskyy.taxi.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final UserService userService;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    public ChatResponse createChat(ChatRequest request, String currentUserPhone) {
        var currentUser = userService.findByPhoneNumber(currentUserPhone);
        
        // Check if chat already exists for this ride
        var existingChat = chatRepository.findByRideIdAndIsActive(request.getRideId(), true);
        if (existingChat.isPresent()) {
            return mapToChatResponse(existingChat.get(), currentUser.getId());
        }

        // Ensure current user is in participants
        if (!request.getParticipantIds().contains(currentUser.getId())) {
            request.getParticipantIds().add(currentUser.getId());
        }

        var chat = Chat.builder()
                .rideId(request.getRideId())
                .participantIds(request.getParticipantIds())
                .createdDate(LocalDateTime.now())
                .lastMessageDate(LocalDateTime.now())
                .isActive(true)
                .build();

        var savedChat = chatRepository.save(chat);
        log.info("Created chat: {}", savedChat);

        // Send system message
        sendSystemMessage(savedChat.getId(), "Chat created for ride sharing");

        return mapToChatResponse(savedChat, currentUser.getId());
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

        var messageResponse = mapToMessageResponse(savedMessage);

        // Send to WebSocket subscribers
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, messageResponse);

        log.info("Sent message: {}", savedMessage);
        return messageResponse;
    }

    public List<ChatResponse> getUserChats(String userPhone) {
        var user = userService.findByPhoneNumber(userPhone);
        var chats = chatRepository.findByParticipantIdsContainingAndIsActive(user.getId(), true);
        
        return chats.stream()
                .map(chat -> mapToChatResponse(chat, user.getId()))
                .collect(Collectors.toList());
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

        return messages.stream()
                .map(this::mapToMessageResponse)
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

    private ChatResponse mapToChatResponse(Chat chat, String currentUserId) {
        var participants = chat.getParticipantIds().stream()
                .map(userService::findUser)
                .collect(Collectors.toList());

        var lastMessage = messageRepository.findByChatIdOrderByTimestampDesc(chat.getId(), PageRequest.of(0, 1))
                .stream().findFirst()
                .map(this::mapToMessageResponse)
                .orElse(null);

        var unreadCount = messageRepository.countByChatIdAndSenderIdNotAndIsReadFalse(chat.getId(), currentUserId);

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
    }

    private MessageResponse mapToMessageResponse(Message message) {
        var sender = "system".equals(message.getSenderId()) ? null : userService.findUser(message.getSenderId());
        
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