package com.tustanovskyy.taxi.service.notification.telegram;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;

class TelegramRidePublisherTest {

    private static final String CHANNEL = "@hurtom_rides";
    private static final String JOIN_URL = "https://example.com/app";
    // Run background work inline so the test can assert on it right away.
    private static final Executor INLINE = Runnable::run;

    private TelegramClient client;
    private MongoTemplate mongoTemplate;
    private MutableClock clock;

    @BeforeEach
    void setUp() {
        client = mock(TelegramClient.class);
        mongoTemplate = mock(MongoTemplate.class);
        clock = new MutableClock(Instant.parse("2026-10-25T10:00:00Z"));
    }

    private TelegramRidePublisher publisher(boolean enabled, int maxPostsPerHour) {
        return new TelegramRidePublisher(client, mongoTemplate, INLINE, clock, enabled, CHANNEL, JOIN_URL,
                maxPostsPerHour, ZoneId.of("Europe/Kyiv"));
    }

    private static Ride ride(String id) {
        return Ride.builder().id(id).isActive(true).isScheduled(false)
                .placeFrom(Place.builder().name("Залізничний вокзал, Київ, Україна").build())
                .placeTo(Place.builder().name("Аеропорт Бориспіль, Бориспіль, Україна").build())
                .build();
    }

    private void storedAs(Ride stored) {
        when(mongoTemplate.findAndModify(any(Query.class), any(UpdateDefinition.class), any(FindAndModifyOptions.class),
                eq(Ride.class))).thenReturn(stored);
    }

    @Test
    void doesNothingWhenDisabled() {
        TelegramRidePublisher publisher = publisher(false, 20);

        publisher.publishNewRide(ride("r1"));
        Ride ended = ride("r1");
        ended.setTelegramMessageId(7L);
        publisher.markRideEnded(ended);

        verifyNoInteractions(client, mongoTemplate);
    }

    @Test
    void postsNewRideWithJoinButtonAndStoresTheMessageId() {
        when(client.sendMessage(eq(CHANNEL), anyString(), eq("Приєднатися"), eq(JOIN_URL))).thenReturn(101L);
        storedAs(ride("r1"));

        publisher(true, 20).publishNewRide(ride("r1"));

        verify(client).sendMessage(eq(CHANNEL), startsWith("#Київ\n🚕 Шукаю попутника"), eq("Приєднатися"), eq(JOIN_URL));
        verify(mongoTemplate).findAndModify(any(Query.class), any(UpdateDefinition.class),
                any(FindAndModifyOptions.class), eq(Ride.class));
        verify(client, never()).editMessageText(anyString(), anyLong(), anyString());
    }

    @Test
    void marksThePostRightAwayIfTheRideEndedWhileItWasBeingPosted() {
        when(client.sendMessage(anyString(), anyString(), anyString(), anyString())).thenReturn(101L);
        Ride alreadyCancelled = ride("r1");
        alreadyCancelled.setIsActive(false);
        storedAs(alreadyCancelled);

        publisher(true, 20).publishNewRide(ride("r1"));

        verify(client).editMessageText(eq(CHANNEL), eq(101L), startsWith("⛔ Вже неактуально"));
    }

    @Test
    void doesNotStoreAnythingWhenPostingFails() {
        when(client.sendMessage(anyString(), anyString(), anyString(), anyString())).thenReturn(null);

        publisher(true, 20).publishNewRide(ride("r1"));

        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void endedRideEditsItsPost() {
        Ride ended = ride("r1");
        ended.setIsActive(false);
        ended.setIsCompleted(true);
        ended.setAgreedPartnerUserId("partner");
        ended.setTelegramMessageId(101L);

        publisher(true, 20).markRideEnded(ended);

        verify(client).editMessageText(eq(CHANNEL), eq(101L), startsWith("✅ Попутника знайдено"));
    }

    @Test
    void endedRideWithoutAPostIsIgnored() {
        publisher(true, 20).markRideEnded(ride("r1"));

        verifyNoInteractions(client);
    }

    @Test
    void capsPostsPerHour() {
        when(client.sendMessage(anyString(), anyString(), anyString(), anyString())).thenReturn(null);
        TelegramRidePublisher publisher = publisher(true, 2);

        publisher.publishNewRide(ride("r1"));
        publisher.publishNewRide(ride("r2"));
        publisher.publishNewRide(ride("r3"));
        verify(client, times(2)).sendMessage(anyString(), anyString(), anyString(), anyString());

        clock.advance(Duration.ofMinutes(61));
        publisher.publishNewRide(ride("r4"));
        verify(client, times(3)).sendMessage(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void neverThrowsIntoTheCaller() {
        when(client.sendMessage(anyString(), anyString(), anyString(), anyString())).thenThrow(new RuntimeException("boom"));
        TelegramRidePublisher publisher = publisher(true, 20);
        TelegramRidePublisher rejecting = new TelegramRidePublisher(client, mongoTemplate,
                task -> { throw new IllegalStateException("executor shut down"); }, clock, true, CHANNEL, JOIN_URL, 20,
                ZoneId.of("Europe/Kyiv"));

        assertDoesNotThrow(() -> publisher.publishNewRide(ride("r1")));
        assertDoesNotThrow(() -> rejecting.publishNewRide(ride("r2")));
    }

    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(now, zone);
        }
    }
}
