package com.tustanovskyy.taxi.domain.request;

import com.tustanovskyy.taxi.document.Message;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private String content;
    private Message.MessageType type = Message.MessageType.TEXT;
} 