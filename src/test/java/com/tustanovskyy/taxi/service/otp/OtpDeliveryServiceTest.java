package com.tustanovskyy.taxi.service.otp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.repository.OtpSendLogRepository;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OtpDeliveryServiceTest {

    private static final String PHONE = "+380501234567";
    private static final Duration TTL = Duration.ofMinutes(5);
    private static final long SMS_DAILY_LIMIT = 500;

    private SmsFlyClient sms;
    private OtpSendLogRepository sendLogRepository;
    private OtpDeliveryService deliveryService;

    @BeforeEach
    void setUp() {
        sms = mock(SmsFlyClient.class);
        sendLogRepository = mock(OtpSendLogRepository.class);
        deliveryService = new OtpDeliveryService(sms, sendLogRepository, SMS_DAILY_LIMIT);
    }

    private void smsWorks(boolean works) {
        when(sms.send(anyString(), anyString(), any())).thenReturn(works);
    }

    private void assertUnavailable() {
        ValidationException e = assertThrows(ValidationException.class,
                () -> deliveryService.deliver(PHONE, "123456", TTL));
        assertEquals(ErrorCode.PHONE_VERIFICATION_UNAVAILABLE, e.getCode());
    }

    @Test
    void sendsTheCodeBySmsAndCountsItTowardsTheDailyCap() {
        smsWorks(true);

        deliveryService.deliver(PHONE, "123456", TTL);

        verify(sms).send(eq(PHONE), contains("123456"), eq(TTL));
        verify(sendLogRepository).save(argThat(row -> OtpDeliveryService.SMS_LOG_KEY.equals(row.getKey())));
    }

    @Test
    void failsWhenTheSmsProviderRejectsTheMessage() {
        smsWorks(false);

        assertUnavailable();
        verify(sendLogRepository, never()).save(any());
    }

    @Test
    void noSmsIsSentOnceTheDailyCapIsReached() {
        when(sendLogRepository.countByKeyAndCreatedAtAfter(eq(OtpDeliveryService.SMS_LOG_KEY), any()))
                .thenReturn(SMS_DAILY_LIMIT);

        assertUnavailable();
        verify(sms, never()).send(anyString(), anyString(), any());
    }
}
