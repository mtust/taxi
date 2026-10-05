package com.tustanovskyy.taxi.service.otp;

import com.tustanovskyy.taxi.document.OtpSendLog;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.OtpSendLogRepository;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sends verification codes by SMS. SMS goes to any country, so as a safety net against
 * SMS-pumping fraud the total number of SMS is capped per 24 hours ({@code otp.sms-daily-limit}).
 */
@Service
@Slf4j
public class OtpDeliveryService {

    /** Rate-limit key every sent SMS is counted under - see {@link OtpSendLog}. */
    static final String SMS_LOG_KEY = "sms";

    private final SmsFlyClient smsFlyClient;
    private final OtpSendLogRepository sendLogRepository;
    private final long smsDailyLimit;

    public OtpDeliveryService(SmsFlyClient smsFlyClient,
                              OtpSendLogRepository sendLogRepository,
                              @Value("${otp.sms-daily-limit}") long smsDailyLimit) {
        this.smsFlyClient = smsFlyClient;
        this.sendLogRepository = sendLogRepository;
        this.smsDailyLimit = smsDailyLimit;
    }

    /**
     * @throws ValidationException PHONE_VERIFICATION_UNAVAILABLE if the code couldn't be sent
     */
    public void deliver(String phoneNumber, String code, Duration ttl) {
        Instant now = Instant.now();
        if (sendLogRepository.countByKeyAndCreatedAtAfter(SMS_LOG_KEY, now.minus(Duration.ofDays(1))) >= smsDailyLimit) {
            log.error("Daily SMS limit of {} reached - not sending SMS until older ones age out. "
                    + "If this isn't an attack, raise OTP_SMS_DAILY_LIMIT", smsDailyLimit);
            throw unavailable();
        }
        if (!smsFlyClient.send(phoneNumber, "HurtomTaxi code: " + code, ttl)) {
            throw unavailable();
        }
        sendLogRepository.save(new OtpSendLog().setKey(SMS_LOG_KEY).setCreatedAt(now));
    }

    private ValidationException unavailable() {
        return new ValidationException(ErrorCode.PHONE_VERIFICATION_UNAVAILABLE,
                "Could not deliver a verification code to this phone number");
    }
}
