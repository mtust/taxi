package com.tustanovskyy.taxi.exception;

import java.util.Map;

public class SmsRateLimitException extends ValidationException {

    public SmsRateLimitException(long retryAfterSeconds) {
        super(ErrorCode.SMS_RATE_LIMITED,
                "SMS was sent too recently. Try again in " + retryAfterSeconds + " seconds",
                Map.of("retryAfterSeconds", retryAfterSeconds));
    }
}
