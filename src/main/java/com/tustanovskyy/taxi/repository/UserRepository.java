package com.tustanovskyy.taxi.repository;


import com.tustanovskyy.taxi.document.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<User, String> {

    Optional<User> findByPhoneNumber(String phoneNumber);

    /**
     * Accounts that were soft-deleted before the given cutoff and are therefore due to have their
     * data (and remaining related rides/chats/messages/ratings) permanently purged.
     */
    List<User> findByDeletedTrueAndDeletedAtBefore(LocalDateTime cutoff);

}
