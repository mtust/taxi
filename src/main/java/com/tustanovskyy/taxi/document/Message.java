package com.tustanovskyy.taxi.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {
    @Id
    private String id;

    private String chatId;
    private String senderId; // User ID of message sender
    private String content;
    private MessageType type; // TEXT, IMAGE, LOCATION, etc.
    
    @CreatedDate
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss")
    private LocalDateTime timestamp;
    
    private boolean isRead;

    public enum MessageType {
        TEXT, IMAGE, LOCATION, SYSTEM
    }
} 