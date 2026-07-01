package com.tustanovskyy.taxi.document;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document
@CompoundIndexes({
    @CompoundIndex(name = "chat_participants_active", def = "{'participantIds': 1, 'isActive': 1}"),
    @CompoundIndex(name = "chat_ride_active", def = "{'rideId': 1, 'isActive': 1}")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chat {
    @Id
    private String id;

    private String rideId;
    private List<String> participantIds; // User IDs of chat participants
    
    @CreatedDate
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime createdDate;

    @LastModifiedDate
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm")
    private LocalDateTime lastMessageDate;
    
    private boolean isActive;
} 