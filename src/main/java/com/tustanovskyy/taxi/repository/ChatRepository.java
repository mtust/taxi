package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRepository extends MongoRepository<Chat, String> {

    /**
     * The user's inbox - paginated and sorted by most-recently-active chat first, so it stays
     * cheap regardless of how many chats a user has accumulated. Covered end-to-end (filter +
     * sort) by the "chat_participants_active_lastmsg" compound index.
     */
    List<Chat> findByParticipantIdsContainingAndIsActiveOrderByLastMessageDateDesc(
            String participantId, boolean isActive, Pageable pageable);

    /**
     * Every chat a user is part of, active or not - used to purge a user's data once their
     * soft-deleted account's retention window has elapsed (see UserService#purgeUserData).
     */
    List<Chat> findByParticipantIdsContaining(String participantId);

    List<Chat> findByRideIdAndParticipantIdsContainingAndIsActive(String rideId, String participantId, boolean isActive);

    /**
     * A ride can have chats with several different partners, so an active chat must be
     * identified by the exact set of participants, not just the rideId.
     */
    @Query("{ 'rideId': ?0, 'participantIds': { '$all': ?1, '$size': ?2 }, 'isActive': ?3 }")
    Optional<Chat> findActiveChatForParticipants(String rideId, List<String> participantIds, int participantCount, boolean isActive);
} 