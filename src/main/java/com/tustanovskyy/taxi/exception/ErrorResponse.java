package com.tustanovskyy.taxi.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private int status;
    private String message;
    private long timestamp;
    private Long retryAfterSeconds;

    public ErrorResponse(int status, String message, long timestamp) {
        this(status, message, timestamp, null);
    }
}
