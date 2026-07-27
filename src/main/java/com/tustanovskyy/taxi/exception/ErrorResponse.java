package com.tustanovskyy.taxi.exception;

import java.util.Map;
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
    /**
     * Stable {@link ErrorCode} name the client can map to a localized message. Null for
     * responses produced outside {@link GlobalExceptionHandler} (there should be none).
     */
    private String code;
    /**
     * Dynamic values referenced by the localized message template (e.g. {@code retryAfterSeconds},
     * a user's name, an entity id) so the FE can interpolate them without parsing {@code message}.
     */
    private Map<String, Object> params;

    public ErrorResponse(int status, String message, long timestamp) {
        this(status, message, timestamp, null, null, Map.of());
    }
}
