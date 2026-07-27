package com.tustanovskyy.taxi.exception;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@Getter
@ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
@Slf4j
public class SmsRateLimitException extends RuntimeException {

    private final long retryAfterSeconds;

    public SmsRateLimitException(long retryAfterSeconds) {
        super("SMS was sent too recently. Try again in " + retryAfterSeconds + " seconds");
        this.retryAfterSeconds = retryAfterSeconds;
        log.warn(getMessage());
    }
}
