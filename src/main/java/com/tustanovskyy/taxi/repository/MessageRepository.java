package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByChatIdOrderByTimestampDesc(String chatId, Pageable pageable);

    List<Message> findByChatIdOrderByTimestampDesc(String chatId);

    /**
     * Used for incremental polling: fetch only messages newer than the client's last-known
     * timestamp, covered by the same {chatId, timestamp} compound index as the paginated query.
     */
    List<Message> findByChatIdAndTimestampAfterOrderByTimestampAsc(String chatId, LocalDateTime since);

    int countByChatIdAndSenderIdNotAndIsReadFalse(String chatId, String senderId);

    List<Message> findByChatIdAndSenderIdNotAndIsReadFalse(String chatId, String senderId);

    /**
     * Whether a chat has any actual participant-authored message - a chat gets created (with
     * only an auto-generated SYSTEM "matched for ride sharing" message) as soon as either side
     * opens the chat screen, before either of them has typed anything, so "a Chat document
     * exists" alone isn't "they started chatting" - see RideService#findPartnersRide.
     */
    boolean existsByChatIdAndTypeNot(String chatId, Message.MessageType type);

    /**
     * Used to purge all messages of the chats a user was part of once their soft-deleted
     * account's retention window has elapsed (see UserService#purgeUserData).
     */
    void deleteByChatIdIn(Collection<String> chatIds);
} 