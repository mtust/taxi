package com.tustanovskyy.taxi.service.otp;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tustanovskyy.taxi.document.OtpCode;
import com.tustanovskyy.taxi.document.OtpSendLog;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.SmsRateLimitException;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.OtpSendLogRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

class OtpServiceTest {

    private static final String PHONE = "+380501234567";
    private static final String IP = "203.0.113.7";
    private static final int MAX_ATTEMPTS = 5;

    private MongoTemplate mongoTemplate;
    private OtpSendLogRepository sendLogRepository;
    private OtpDeliveryService deliveryService;
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        sendLogRepository = mock(OtpSendLogRepository.class);
        deliveryService = mock(OtpDeliveryService.class);
        otpService = new OtpService(mongoTemplate, sendLogRepository, deliveryService,
                "test-secret", 5, MAX_ATTEMPTS, List.of(60L, 60L, 180L, 300L, 3600L), 30);

        when(sendLogRepository.findFirstByKeyOrderByCreatedAtDesc(anyString())).thenReturn(Optional.empty());
    }

    /** Sends a code and returns what was delivered to the user plus what was stored. */
    private Sent send() {
        otpService.sendCode(PHONE, IP);
        ArgumentCaptor<String> code = ArgumentCaptor.forClass(String.class);
        verify(deliveryService).deliver(eq(PHONE), code.capture(), eq(Duration.ofMinutes(5)));
        ArgumentCaptor<OtpCode> stored = ArgumentCaptor.forClass(OtpCode.class);
        verify(mongoTemplate).save(stored.capture());
        return new Sent(code.getValue(), stored.getValue());
    }

    private record Sent(String code, OtpCode stored) {
    }

    /** Makes the next verify() see the stored code with the given (already incremented) attempt count. */
    private void storedCodeHasAttempts(OtpCode stored, int attempts) {
        when(mongoTemplate.findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(OtpCode.class)))
                .thenReturn(stored.setAttempts(attempts));
    }

    private void assertInvalidCode(String code) {
        ValidationException e = assertThrows(ValidationException.class, () -> otpService.verify(PHONE, code));
        assertEquals(ErrorCode.INVALID_VERIFICATION_CODE, e.getCode());
    }

    @Test
    void storesOnlyAHashOfASixDigitCode() {
        Sent sent = send();

        assertTrue(sent.code().matches("\\d{6}"));
        assertEquals(PHONE, sent.stored().getPhoneNumber());
        assertFalse(sent.stored().getCodeHash().contains(sent.code()));
        assertTrue(sent.stored().getExpiresAt().isAfter(Instant.now().plus(Duration.ofMinutes(4))));
    }

    @Test
    void correctCodeVerifiesAndIsConsumed() {
        Sent sent = send();
        storedCodeHasAttempts(sent.stored(), 1);

        assertDoesNotThrow(() -> otpService.verify(PHONE, sent.code()));
        verify(mongoTemplate).remove(any(Query.class), eq(OtpCode.class));
    }

    @Test
    void wrongCodeIsRejectedButKeptForAnotherTry() {
        Sent sent = send();
        storedCodeHasAttempts(sent.stored(), 1);

        assertInvalidCode(sent.code().equals("000000") ? "111111" : "000000");
        verify(mongoTemplate, never()).remove(any(Query.class), eq(OtpCode.class));
    }

    @Test
    void codeIsDiscardedAfterTheLastAllowedWrongAttempt() {
        Sent sent = send();
        storedCodeHasAttempts(sent.stored(), MAX_ATTEMPTS);

        assertInvalidCode(sent.code().equals("000000") ? "111111" : "000000");
        verify(mongoTemplate).remove(any(Query.class), eq(OtpCode.class));
    }

    @Test
    void evenTheCorrectCodeIsRejectedOnceAttemptsAreUsedUp() {
        Sent sent = send();
        storedCodeHasAttempts(sent.stored(), MAX_ATTEMPTS + 1);

        assertInvalidCode(sent.code());
    }

    @Test
    void expiredCodeIsRejected() {
        Sent sent = send();
        sent.stored().setExpiresAt(Instant.now().minusSeconds(1));
        storedCodeHasAttempts(sent.stored(), 1);

        assertInvalidCode(sent.code());
    }

    @Test
    void codeForAnotherNumberIsRejected() {
        Sent sent = send();
        storedCodeHasAttempts(sent.stored(), 1);

        assertThrows(ValidationException.class, () -> otpService.verify("+380509999999", sent.code()));
    }

    @Test
    void unknownNumberIsRejected() {
        assertInvalidCode("123456");
    }

    /** The number already got {@code previousCodes} codes in the last day, the latest {@code secondsAgo} ago. */
    private void previousCodes(long previousCodes, long secondsAgo) {
        when(sendLogRepository.countByKeyAndCreatedAtAfter(eq(PHONE), any())).thenReturn(previousCodes);
        when(sendLogRepository.findFirstByKeyOrderByCreatedAtDesc(PHONE)).thenReturn(Optional.of(
                new OtpSendLog().setKey(PHONE).setCreatedAt(Instant.now().minusSeconds(secondsAgo))));
    }

    private void assertMustWait(long expectedSeconds) {
        SmsRateLimitException e = assertThrows(SmsRateLimitException.class, () -> otpService.sendCode(PHONE, IP));
        long retryAfter = ((Number) e.getParams().get("retryAfterSeconds")).longValue();
        assertTrue(retryAfter > expectedSeconds - 5 && retryAfter <= expectedSeconds,
                "retryAfterSeconds was " + retryAfter + ", expected about " + expectedSeconds);
        verify(deliveryService, never()).deliver(anyString(), anyString(), any());
    }

    @Test
    void cooldownEscalatesWithEachCodeSent() {
        previousCodes(1, 20);
        assertMustWait(60 - 20);
        previousCodes(2, 20);
        assertMustWait(60 - 20);
        previousCodes(3, 20);
        assertMustWait(180 - 20);
        previousCodes(4, 20);
        assertMustWait(300 - 20);
        previousCodes(5, 20);
        assertMustWait(3600 - 20);
    }

    @Test
    void lastCooldownStepAppliesToEveryFurtherCode() {
        previousCodes(12, 600);
        assertMustWait(3600 - 600);
    }

    @Test
    void nextCodeIsAllowedOnceTheCooldownHasPassed() {
        previousCodes(3, 181);

        assertDoesNotThrow(() -> otpService.sendCode(PHONE, IP));
    }

    @Test
    void perIpLimitIsEnforced() {
        when(sendLogRepository.countByKeyAndCreatedAtAfter(eq("ip:" + IP), any())).thenReturn(30L);

        assertThrows(SmsRateLimitException.class, () -> otpService.sendCode(PHONE, IP));
        verify(deliveryService, never()).deliver(anyString(), anyString(), any());
    }

    @Test
    void malformedPhoneNumberIsRejected() {
        ValidationException e = assertThrows(ValidationException.class, () -> otpService.sendCode("0501234567", IP));
        assertEquals(ErrorCode.INVALID_PHONE_NUMBER, e.getCode());
    }

    @Test
    void nothingIsStoredWhenDeliveryFails() {
        doThrow(new ValidationException(ErrorCode.PHONE_VERIFICATION_UNAVAILABLE, "unavailable"))
                .when(deliveryService).deliver(anyString(), anyString(), any());

        assertThrows(ValidationException.class, () -> otpService.sendCode(PHONE, IP));
        verify(mongoTemplate, never()).save(any(OtpCode.class));
    }
}
