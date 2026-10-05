package com.tustanovskyy.taxi.repository;

import com.tustanovskyy.taxi.document.OtpSendLog;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OtpSendLogRepository extends MongoRepository<OtpSendLog, String> {

    long countByKeyAndCreatedAtAfter(String key, Instant since);

    Optional<OtpSendLog> findFirstByKeyOrderByCreatedAtDesc(String key);

    Optional<OtpSendLog> findFirstByKeyAndCreatedAtAfterOrderByCreatedAtAsc(String key, Instant since);
}
