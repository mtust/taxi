package com.tustanovskyy.taxi.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Prevents SMS spam by enforcing an escalating cooldown between verification SMS sends,
 * based on the last {@value #HISTORY_SIZE} send timestamps of a user (sliding window):
 * <ul>
 *     <li>after the 1st send  -> next one is allowed after 1 minute</li>
 *     <li>after the 2nd send  -> next one is allowed after 5 minutes</li>
 *     <li>after the 3rd send (and any further one) -> next one is allowed after 10 minutes</li>
 * </ul>
 */
@Component
public class SmsRateLimiter {

    private static final int HISTORY_SIZE = 3;
    private static final List<Duration> COOLDOWNS = List.of(
            Duration.ofMinutes(1),
            Duration.ofMinutes(5),
            Duration.ofMinutes(10)
    );

    /**
     * @return how many seconds must still pass before a new SMS may be sent, or 0 if allowed now.
     */
    public long secondsUntilNextAllowedSend(List<LocalDateTime> sentAtHistory) {
        if (sentAtHistory == null || sentAtHistory.isEmpty()) {
            return 0;
        }
        Duration requiredCooldown = COOLDOWNS.get(Math.min(sentAtHistory.size(), COOLDOWNS.size()) - 1);
        LocalDateTime lastSentAt = sentAtHistory.get(sentAtHistory.size() - 1);
        long secondsRemaining = Duration.between(LocalDateTime.now(), lastSentAt.plus(requiredCooldown)).getSeconds();
        return Math.max(secondsRemaining, 0);
    }

    /**
     * Appends "now" to the history and keeps only the last {@value #HISTORY_SIZE} entries.
     */
    public List<LocalDateTime> registerSend(List<LocalDateTime> sentAtHistory) {
        List<LocalDateTime> updated = new ArrayList<>(sentAtHistory == null ? List.of() : sentAtHistory);
        updated.add(LocalDateTime.now());
        while (updated.size() > HISTORY_SIZE) {
            updated.remove(0);
        }
        return updated;
    }
}
