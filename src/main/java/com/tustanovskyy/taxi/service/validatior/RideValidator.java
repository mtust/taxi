package com.tustanovskyy.taxi.service.validatior;

import com.tustanovskyy.taxi.exception.ErrorCode;
import java.time.Duration;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

@Component
public class RideValidator extends BaseValidator {

    /**
     * Validates a user-picked schedule window (see RideRequest#scheduledFrom/scheduledTo).
     *
     * @param maxAdvanceHours how far in the future {@code scheduledFrom} is allowed to start
     * @param maxWindowMinutes maximum allowed length of the window
     */
    public void validateSchedule(LocalDateTime scheduledFrom, LocalDateTime scheduledTo,
                                  int maxAdvanceHours, int maxWindowMinutes) {
        LocalDateTime now = LocalDateTime.now();
        validate(() -> scheduledFrom != null && scheduledTo != null,
                ErrorCode.RIDE_SCHEDULE_WINDOW_INVALID, "Scheduled window requires both a start and an end");
        validate(() -> !scheduledFrom.isBefore(now.minusMinutes(1)),
                ErrorCode.RIDE_SCHEDULE_IN_PAST, "Scheduled start time is in the past");
        validate(() -> !scheduledFrom.isAfter(now.plusHours(maxAdvanceHours)),
                ErrorCode.RIDE_SCHEDULE_TOO_FAR_AHEAD, "Scheduled start time is more than "
                        + maxAdvanceHours + " hours away");
        validate(() -> scheduledTo.isAfter(scheduledFrom),
                ErrorCode.RIDE_SCHEDULE_WINDOW_INVALID, "Scheduled end must be after the start");
        validate(() -> Duration.between(scheduledFrom, scheduledTo).toMinutes() <= maxWindowMinutes,
                ErrorCode.RIDE_SCHEDULE_WINDOW_INVALID, "Scheduled window is longer than "
                        + maxWindowMinutes + " minutes");
    }
}
