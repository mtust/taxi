package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Chat;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

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
     * The same two people always share one chat regardless of which (possibly many, over time)
     * ride matched them together - not scoped by rideId, so re-matching later reuses this same
     * conversation instead of starting a new one. An active chat is identified by the exact set
     * of participants.
     *
     * Returns a List, not a single Optional: chats created before this pairing-only dedup existed
     * were scoped per-ride, so the same two people can have several still-"active" chats left
     * over from matching on different rides in the past - see ChatService#createChat, which
     * picks the most recent of these and deactivates the rest.
     */
    @Query("{ 'participantIds': { '$all': ?0, '$size': ?1 }, 'isActive': ?2 }")
    List<Chat> findActiveChatsForParticipants(List<String> participantIds, int participantCount, boolean isActive);
} 