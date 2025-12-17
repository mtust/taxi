package com.tustanovskyy.taxi.domain.response;

import com.tustanovskyy.taxi.document.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatResponse {
    private String id;
    private String rideId;
    private List<User> participants;
    private MessageResponse lastMessage;
    private LocalDateTime createdDate;
    private LocalDateTime lastMessageDate;
    private boolean isActive;
    private int unreadCount;
} 