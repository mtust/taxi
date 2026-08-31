package com.tustanovskyy.taxi.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Stable, language-independent identifiers for every business error the API can return.
 * The FE maps {@code code} to a localized message (falling back to {@code message} from
 * {@link ErrorResponse} if a translation is missing), so this name is part of the API contract
 * and must not change once released.
 */
@Getter
public enum ErrorCode {
    PASSWORDS_DO_NOT_MATCH(HttpStatus.BAD_REQUEST),
    PHONE_NUMBER_EMPTY(HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(HttpStatus.CONFLICT),
    PHONE_NOT_VERIFIED(HttpStatus.BAD_REQUEST),
    /**
     * Deliberately shared by "no such phone number" and "wrong password" during login, so the
     * response never discloses which one was incorrect.
     */
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND),
    ACCESS_DENIED(HttpStatus.FORBIDDEN),
    ADD_HOME_ADDRESS_FAILED(HttpStatus.BAD_REQUEST),
    SMS_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS),
    USER_HAS_ACTIVE_RIDE(HttpStatus.CONFLICT),
    RIDE_NOT_FOUND(HttpStatus.NOT_FOUND),
    RIDE_SCHEDULE_IN_PAST(HttpStatus.BAD_REQUEST),
    RIDE_SCHEDULE_TOO_FAR_AHEAD(HttpStatus.BAD_REQUEST),
    RIDE_SCHEDULE_WINDOW_INVALID(HttpStatus.BAD_REQUEST),
    CHAT_NOT_FOUND(HttpStatus.NOT_FOUND),
    NOT_CHAT_PARTICIPANT(HttpStatus.FORBIDDEN),
    ALREADY_RATED(HttpStatus.CONFLICT),
    INVALID_RATING_SCORE(HttpStatus.BAD_REQUEST),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED),
    FEEDBACK_MESSAGE_EMPTY(HttpStatus.BAD_REQUEST),
    PARTNER_USER_ID_REQUIRED(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus httpStatus;

    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }
}
