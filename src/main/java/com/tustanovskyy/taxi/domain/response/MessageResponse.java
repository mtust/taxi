package com.tustanovskyy.taxi.domain.response;

import com.tustanovskyy.taxi.document.Message;
import com.tustanovskyy.taxi.document.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageResponse {
    private String id;
    private String chatId;
    private User sender;
    private String content;
    private Message.MessageType type;
    private LocalDateTime timestamp;
    private boolean isRead;
} 