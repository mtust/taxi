package com.tustanovskyy.taxi.service.otp;

import com.tustanovskyy.taxi.document.OtpCode;
import com.tustanovskyy.taxi.document.OtpSendLog;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.SmsRateLimitException;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.OtpSendLogRepository;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

/**
 * Phone verification codes, generated and checked by this backend: a random 6-digit code is sent
 * to the user (see {@link OtpDeliveryService}) and only its HMAC is stored, with a short TTL and a
 * cap on check attempts. Requests for a code are rate limited per phone number and per caller IP.
 */
@Service
@Slf4j
public class OtpService {

    private static final Pattern E164 = Pattern.compile("^\\+[1-9]\\d{7,14}$");
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Duration DAY = Duration.ofDays(1);
    private static final Duration HOUR = Duration.ofHours(1);

    private final MongoTemplate mongoTemplate;
    private final OtpSendLogRepository sendLogRepository;
    private final OtpDeliveryService deliveryService;
    private final SecureRandom secureRandom = new SecureRandom();

    private final byte[] secret;
    private final Duration ttl;
    private final int maxAttempts;
    private final List<Duration> resendCooldowns;
    private final int maxPerIpPerHour;

    public OtpService(MongoTemplate mongoTemplate,
                      OtpSendLogRepository sendLogRepository,
                      OtpDeliveryService deliveryService,
                      @Value("${otp.secret}") String secret,
                      @Value("${otp.ttl-minutes}") long ttlMinutes,
                      @Value("${otp.max-attempts}") int maxAttempts,
                      @Value("${otp.resend-cooldowns-seconds}") List<Long> resendCooldownsSeconds,
                      @Value("${otp.max-per-ip-per-hour}") int maxPerIpPerHour) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("otp.secret (OTP_SECRET) must be set");
        }
        this.mongoTemplate = mongoTemplate;
        this.sendLogRepository = sendLogRepository;
        this.deliveryService = deliveryService;
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.maxAttempts = maxAttempts;
        this.resendCooldowns = resendCooldownsSeconds.stream().map(Duration::ofSeconds).toList();
        this.maxPerIpPerHour = maxPerIpPerHour;
    }

    /**
     * Generates a new code for the phone number (replacing any previous one) and sends it.
     *
     * @throws SmsRateLimitException if this number or IP has requested codes too often
     * @throws ValidationException PHONE_VERIFICATION_UNAVAILABLE if it couldn't be delivered
     */
    public void sendCode(String phoneNumber, String clientIp) {
        registerRequest(phoneNumber, clientIp);

        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        deliveryService.deliver(phoneNumber, code, ttl);

        mongoTemplate.save(new OtpCode()
                .setPhoneNumber(phoneNumber)
                .setCodeHash(hash(phoneNumber, code))
                .setAttempts(0)
                .setExpiresAt(Instant.now().plus(ttl)));
        log.info("Verification code sent to {}", phoneNumber);
    }

    /**
     * Applies the rate limits and counts this request against them, without sending anything.
     * Called on its own for requests we answer but don't act on (e.g. forgot-password for an
     * unknown number), so those are throttled exactly like real ones and the response doesn't
     * give away which numbers are registered.
     */
    public void registerRequest(String phoneNumber, String clientIp) {
        if (phoneNumber == null || !E164.matcher(phoneNumber).matches()) {
            throw new ValidationException(ErrorCode.INVALID_PHONE_NUMBER, "Invalid phone number");
        }
        Instant now = Instant.now();
        String ipKey = "ip:" + clientIp;

        rejectIfInCooldown(phoneNumber, now);
        rejectIfOverLimit(ipKey, maxPerIpPerHour, HOUR, now);

        sendLogRepository.save(new OtpSendLog().setKey(phoneNumber).setCreatedAt(now));
        sendLogRepository.save(new OtpSendLog().setKey(ipKey).setCreatedAt(now));
    }

    /**
     * Checks a user-submitted code. A code is single-use, and is thrown away after
     * {@code otp.max-attempts} wrong guesses - the user has to request a new one.
     *
     * @throws ValidationException INVALID_VERIFICATION_CODE if it's wrong, expired, or used up
     */
    public void verify(String phoneNumber, String code) {
        if (phoneNumber == null || code == null) {
            throw invalidCode();
        }
        Query byPhone = Query.query(Criteria.where("_id").is(phoneNumber));
        // Counting the attempt atomically, before comparing, so parallel guesses can't each see
        // "attempts left" and slip past the cap.
        OtpCode otp = mongoTemplate.findAndModify(byPhone, new Update().inc("attempts", 1),
                FindAndModifyOptions.options().returnNew(true), OtpCode.class);
        if (otp == null) {
            throw invalidCode();
        }
        if (otp.getExpiresAt().isBefore(Instant.now()) || otp.getAttempts() > maxAttempts) {
            mongoTemplate.remove(byPhone, OtpCode.class);
            throw invalidCode();
        }
        boolean matches = MessageDigest.isEqual(
                otp.getCodeHash().getBytes(StandardCharsets.UTF_8),
                hash(phoneNumber, code).getBytes(StandardCharsets.UTF_8));
        if (!matches) {
            if (otp.getAttempts() >= maxAttempts) {
                mongoTemplate.remove(byPhone, OtpCode.class);
            }
            throw invalidCode();
        }
        mongoTemplate.remove(byPhone, OtpCode.class);
    }

    /**
     * Escalating cooldown between codes for one number: the more codes it was sent in the last
     * 24 hours, the longer the wait before the next ({@code otp.resend-cooldowns-seconds} - the
     * last step applies to every further request). It eases off again as old requests age out.
     */
    private void rejectIfInCooldown(String phoneNumber, Instant now) {
        long recentRequests = sendLogRepository.countByKeyAndCreatedAtAfter(phoneNumber, now.minus(DAY));
        if (recentRequests == 0) {
            return;
        }
        Duration cooldown = resendCooldowns.get((int) Math.min(recentRequests, resendCooldowns.size()) - 1);
        sendLogRepository.findFirstByKeyOrderByCreatedAtDesc(phoneNumber)
                .ifPresent(last -> rejectIfBefore(last.getCreatedAt().plus(cooldown), now));
    }

    private void rejectIfOverLimit(String key, int limit, Duration window, Instant now) {
        Instant windowStart = now.minus(window);
        if (sendLogRepository.countByKeyAndCreatedAtAfter(key, windowStart) < limit) {
            return;
        }
        // Allowed again once the oldest request in the window falls out of it.
        Instant allowedAt = sendLogRepository.findFirstByKeyAndCreatedAtAfterOrderByCreatedAtAsc(key, windowStart)
                .map(oldest -> oldest.getCreatedAt().plus(window))
                .orElse(now.plus(window));
        rejectIfBefore(allowedAt, now);
    }

    private void rejectIfBefore(Instant allowedAt, Instant now) {
        long secondsRemaining = Duration.between(now, allowedAt).toSeconds();
        if (secondsRemaining > 0) {
            throw new SmsRateLimitException(secondsRemaining);
        }
    }

    // Keyed (HMAC) rather than a plain hash: a 6-digit code has only a million possible values, so
    // an unkeyed hash could be reversed instantly by anyone who got a look at the database. The
    // phone number is mixed in so a hash can't be replayed for a different number.
    private String hash(String phoneNumber, String code) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return HexFormat.of().formatHex(mac.doFinal((phoneNumber + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private ValidationException invalidCode() {
        return new ValidationException(ErrorCode.INVALID_VERIFICATION_CODE, "Invalid verification code");
    }
}
