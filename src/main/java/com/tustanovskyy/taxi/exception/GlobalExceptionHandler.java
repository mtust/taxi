package com.tustanovskyy.taxi.exception;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(ValidationException ex) {
        Long retryAfterSeconds = null;
        Object rawRetryAfter = ex.getParams().get("retryAfterSeconds");
        if (rawRetryAfter instanceof Number number) {
            retryAfterSeconds = number.longValue();
        }

        ErrorResponse errorResponse = new ErrorResponse(
                ex.getCode().getHttpStatus().value(),
                ex.getMessage(),
                System.currentTimeMillis(),
                retryAfterSeconds,
                ex.getCode().name(),
                ex.getParams()
        );
        return new ResponseEntity<>(errorResponse, ex.getCode().getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        log.error("Unhandled exception", ex);
        ErrorResponse errorResponse = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage(),
                System.currentTimeMillis(),
                null,
                ErrorCode.INTERNAL_ERROR.name(),
                Map.of()
        );

        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
