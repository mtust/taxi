package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
    
    List<Message> findByChatIdOrderByTimestampDesc(String chatId, Pageable pageable);
    
    List<Message> findByChatIdOrderByTimestampDesc(String chatId);
    
    int countByChatIdAndSenderIdNotAndIsReadFalse(String chatId, String senderId);
    
    List<Message> findByChatIdAndSenderIdNotAndIsReadFalse(String chatId, String senderId);
} 