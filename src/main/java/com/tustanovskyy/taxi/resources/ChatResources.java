package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.request.ChatRequest;
import com.tustanovskyy.taxi.domain.request.MessageRequest;
import com.tustanovskyy.taxi.domain.response.ChatResponse;
import com.tustanovskyy.taxi.domain.response.MessageResponse;
import com.tustanovskyy.taxi.service.ChatService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("chats")
@Slf4j
@AllArgsConstructor
public class ChatResources {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse createChat(@RequestBody ChatRequest request,
                                  @AuthenticationPrincipal String phoneNumber) {
        log.info("Creating chat for user: {}, ride: {}", phoneNumber, request.getRideId());
        return chatService.createChat(request, phoneNumber);
    }

    @GetMapping
    public List<ChatResponse> getUserChats(@RequestParam(required = false) String rideId,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(defaultValue = "10") int size,
                                          @AuthenticationPrincipal String phoneNumber) {
        // Polled every 10s by the app-wide notification listener plus fetched on every inbox
        // screen focus, so log at debug rather than flooding INFO-level logs.
        log.debug("Getting chats for user: {}, ride: {}, page: {}, size: {}", phoneNumber, rideId, page, size);
        return chatService.getUserChats(phoneNumber, rideId, page, size);
    }

    @PostMapping("/{chatId}/messages")
    public MessageResponse sendMessage(@PathVariable String chatId,
                                     @RequestBody MessageRequest request,
                                     @AuthenticationPrincipal String phoneNumber) {
        log.info("Sending message to chat: {} from user: {}", chatId, phoneNumber);
        return chatService.sendMessage(chatId, request, phoneNumber);
    }

    @GetMapping("/{chatId}/messages")
    public List<MessageResponse> getChatMessages(@PathVariable String chatId,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false)
                                               @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime since,
                                               @AuthenticationPrincipal String phoneNumber) {
        log.debug("Getting messages for chat: {} by user: {} since: {}", chatId, phoneNumber, since);
        return chatService.getChatMessages(chatId, phoneNumber, page, size, since);
    }

    @PutMapping("/{chatId}/messages/read")
    public void markMessagesAsRead(@PathVariable String chatId,
                                 @AuthenticationPrincipal String phoneNumber) {
        log.info("Marking messages as read for chat: {} by user: {}", chatId, phoneNumber);
        chatService.markMessagesAsRead(chatId, phoneNumber);
    }
} 