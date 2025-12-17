package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Chat;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends MongoRepository<Chat, String> {
    
    List<Chat> findByParticipantIdsContainingAndIsActive(String participantId, boolean isActive);
    
    Optional<Chat> findByRideIdAndIsActive(String rideId, boolean isActive);
    
    List<Chat> findByRideIdAndParticipantIdsContainingAndIsActive(String rideId, String participantId, boolean isActive);
} 