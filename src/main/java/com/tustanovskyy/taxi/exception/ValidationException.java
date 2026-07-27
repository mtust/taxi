package com.tustanovskyy.taxi.exception;

import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base exception for expected business-rule failures (validation, not-found, access-denied,
 * conflicts, etc). Carries an {@link ErrorCode} that determines both the HTTP status and the
 * key the FE uses to show a localized message - see {@link GlobalExceptionHandler}.
 */
@Getter
@Slf4j
public class ValidationException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> params;

    public ValidationException(ErrorCode code, String message) {
        this(code, message, Map.of());
    }

    public ValidationException(ErrorCode code, String message, Map<String, Object> params) {
        super(message);
        this.code = code;
        this.params = params == null ? Map.of() : params;
        log.error(message);
    }
}
