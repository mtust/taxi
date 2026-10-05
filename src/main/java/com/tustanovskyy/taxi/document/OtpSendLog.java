package com.tustanovskyy.taxi.document;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * One row per counted event, per rate-limit key - see OtpService (a phone number, or "ip:" + the
 * caller's address, per code request) and OtpDeliveryService ("sms", per SMS sent). Kept in Mongo
 * rather than in memory so the limits hold across Cloud Run instances and restarts; rows expire
 * on their own once they're older than the longest window (24h).
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OtpSendLog {

    @Id
    private String id;

    @Indexed
    private String key;

    @Indexed(expireAfter = "1d")
    private Instant createdAt;
}
