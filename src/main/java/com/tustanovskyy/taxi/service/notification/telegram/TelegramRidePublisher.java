package com.tustanovskyy.taxi.service.notification.telegram;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tustanovskyy.taxi.document.Ride;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Publishes new rides to the public Telegram rides channel and marks the post once the ride is
 * over, so the channel shows real activity and brings in new users. See RideTelegramMessages for
 * what a post contains (no names, phones or exact addresses).
 *
 * Off unless telegram.rides.enabled is true AND both the bot token and channel id are set. Calls go
 * out on a background thread, are capped per hour (per instance), and never throw - a Telegram
 * problem must never affect creating, cancelling or completing a ride.
 */
@Service
@Slf4j
public class TelegramRidePublisher {

    private final TelegramClient client;
    private final MongoTemplate mongoTemplate;
    private final Executor executor;
    private final Clock clock;
    private final boolean enabled;
    private final String channelId;
    private final String joinUrl;
    private final int maxPostsPerHour;
    private final ZoneId zone;
    private final Deque<Instant> recentPosts = new ArrayDeque<>();

    @Autowired
    public TelegramRidePublisher(MongoTemplate mongoTemplate,
                                 ObjectMapper objectMapper,
                                 @Value("${telegram.rides.enabled:false}") boolean enabled,
                                 @Value("${telegram.bot-token:}") String botToken,
                                 @Value("${telegram.rides.channel-id:}") String channelId,
                                 @Value("${telegram.rides.join-url:}") String joinUrl,
                                 @Value("${telegram.rides.max-posts-per-hour:20}") int maxPostsPerHour,
                                 @Value("${telegram.rides.time-zone:Europe/Kyiv}") String zone) {
        this(new TelegramClient(new RestTemplate(), objectMapper, botToken), mongoTemplate,
                Executors.newSingleThreadExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "telegram-rides");
                    thread.setDaemon(true);
                    return thread;
                }),
                Clock.systemUTC(),
                enabled && !botToken.isBlank() && !channelId.isBlank(),
                channelId, joinUrl, maxPostsPerHour, ZoneId.of(zone));
        if (enabled && !this.enabled) {
            log.error("telegram.rides.enabled is true but TELEGRAM_BOT_TOKEN / TELEGRAM_CHANNEL_ID is missing - not publishing rides");
        }
    }

    TelegramRidePublisher(TelegramClient client, MongoTemplate mongoTemplate, Executor executor, Clock clock,
                          boolean enabled, String channelId, String joinUrl, int maxPostsPerHour, ZoneId zone) {
        this.client = client;
        this.mongoTemplate = mongoTemplate;
        this.executor = executor;
        this.clock = clock;
        this.enabled = enabled;
        this.channelId = channelId;
        this.joinUrl = joinUrl;
        this.maxPostsPerHour = maxPostsPerHour;
        this.zone = zone;
    }

    public void publishNewRide(Ride ride) {
        if (!enabled || ride.getId() == null) {
            return;
        }
        if (!tryAcquirePostSlot()) {
            log.info("Telegram rides channel: hourly limit of {} posts reached, not posting ride {}", maxPostsPerHour, ride.getId());
            return;
        }
        run(() -> {
            Long messageId = client.sendMessage(channelId, RideTelegramMessages.newRide(ride, zone, today()),
                    RideTelegramMessages.JOIN_BUTTON, joinUrl);
            if (messageId == null) {
                return;
            }
            Ride stored = mongoTemplate.findAndModify(
                    Query.query(Criteria.where("_id").is(ride.getId())),
                    new Update().set("telegramMessageId", messageId),
                    FindAndModifyOptions.options().returnNew(true),
                    Ride.class);
            // The ride may have ended while the post was in flight - its end hook found no message
            // id to edit then, so mark the post now.
            if (stored != null && !Boolean.TRUE.equals(stored.getIsActive())) {
                client.editMessageText(channelId, messageId, RideTelegramMessages.endedRide(stored, zone, today()));
            }
        });
    }

    /** Call after a ride is cancelled, completed or expired (with its final state already set). */
    public void markRideEnded(Ride ride) {
        if (!enabled || ride.getTelegramMessageId() == null) {
            return;
        }
        long messageId = ride.getTelegramMessageId();
        run(() -> client.editMessageText(channelId, messageId, RideTelegramMessages.endedRide(ride, zone, today())));
    }

    private synchronized boolean tryAcquirePostSlot() {
        Instant now = clock.instant();
        while (!recentPosts.isEmpty() && recentPosts.peekFirst().isBefore(now.minus(Duration.ofHours(1)))) {
            recentPosts.pollFirst();
        }
        if (recentPosts.size() >= maxPostsPerHour) {
            return false;
        }
        recentPosts.addLast(now);
        return true;
    }

    private LocalDate today() {
        return LocalDate.now(clock.withZone(zone));
    }

    private void run(Runnable task) {
        try {
            executor.execute(() -> {
                try {
                    task.run();
                } catch (Exception e) {
                    log.warn("Telegram rides channel update failed: {}", e.getClass().getSimpleName());
                }
            });
        } catch (Exception e) {
            log.warn("Could not schedule Telegram rides channel update: {}", e.getMessage());
        }
    }
}
