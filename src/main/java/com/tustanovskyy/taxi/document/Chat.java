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
    // Covers the paginated/sorted inbox query (participantIds + isActive filter, sorted by
    // lastMessageDate desc) so pagination doesn't require an in-memory sort as chat counts grow.
    // Replaces the old "chat_participants_active" index. Spring Data's auto-index-creation only
    // ensures declared indexes exist, it doesn't drop ones that are no longer declared - the old
    // index becomes redundant after deploy and should be dropped manually in Atlas.
    @CompoundIndex(name = "chat_participants_active_lastmsg", def = "{'participantIds': 1, 'isActive': 1, 'lastMessageDate': -1}"),
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