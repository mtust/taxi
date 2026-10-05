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
 * The single pending phone verification code for a phone number - see OtpService. Only an HMAC of
 * the code is stored, never the code itself. Keyed by phone number so issuing a new code replaces
 * (and thereby invalidates) the previous one.
 */
@Document
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class OtpCode {

    @Id
    private String phoneNumber;

    private String codeHash;

    /** Number of check attempts made against this code so far - see OtpService#verify. */
    private int attempts;

    /**
     * Mongo's TTL monitor only sweeps about once a minute, so this is also checked explicitly on
     * verification - the index is just cleanup.
     */
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
}
