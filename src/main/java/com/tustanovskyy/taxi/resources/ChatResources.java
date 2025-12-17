package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.request.ChatRequest;
import com.tustanovskyy.taxi.domain.request.MessageRequest;
import com.tustanovskyy.taxi.domain.response.ChatResponse;
import com.tustanovskyy.taxi.domain.response.MessageResponse;
import com.tustanovskyy.taxi.service.ChatService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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
    public List<ChatResponse> getUserChats(@AuthenticationPrincipal String phoneNumber) {
        log.info("Getting chats for user: {}", phoneNumber);
        return chatService.getUserChats(phoneNumber);
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
                                               @RequestParam(defaultValue = "50") int size,
                                               @AuthenticationPrincipal String phoneNumber) {
        log.info("Getting messages for chat: {} by user: {}", chatId, phoneNumber);
        return chatService.getChatMessages(chatId, phoneNumber, page, size);
    }

    @PutMapping("/{chatId}/messages/read")
    public void markMessagesAsRead(@PathVariable String chatId,
                                 @AuthenticationPrincipal String phoneNumber) {
        log.info("Marking messages as read for chat: {} by user: {}", chatId, phoneNumber);
        chatService.markMessagesAsRead(chatId, phoneNumber);
    }
} 