package com.tustanovskyy.taxi.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.repository.ChatRepository;
import com.tustanovskyy.taxi.repository.MessageRepository;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.service.notification.ExpoPushService;
import com.tustanovskyy.taxi.service.notification.telegram.TelegramRidePublisher;
import com.tustanovskyy.taxi.service.validatior.RideValidator;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.geo.Distance;
import org.bson.Document;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.geo.Sphere;
import org.springframework.data.mongodb.core.query.GeoCommand;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.UpdateDefinition;
import org.springframework.test.util.ReflectionTestUtils;

class RideServicePartnerFoundTest {

    private static final String PHONE = "+380501234567";
    // GeoJSON order, as the FE sends them: x = longitude, y = latitude (Kyiv).
    private static final double X = 30.5234;
    private static final double Y = 50.4501;
    // ~0.0009 degrees of latitude (y) is ~100 m.
    private static final double HUNDRED_METERS = 0.0009;

    private RideRepository rideRepository;
    private ExpoPushService expoPushService;
    private UserService userService;
    private RideMapper rideMapper;
    private MongoTemplate mongoTemplate;
    private TelegramRidePublisher telegramRidePublisher;
    private RideService rideService;

    private final User me = new User().setId("me");
    private final User partner = new User().setId("partner").setPushToken("ExponentPushToken[p]").setLanguage("uk");

    @BeforeEach
    void setUp() {
        rideRepository = mock(RideRepository.class);
        expoPushService = mock(ExpoPushService.class);
        userService = mock(UserService.class);
        rideMapper = mock(RideMapper.class);
        mongoTemplate = mock(MongoTemplate.class);
        telegramRidePublisher = mock(TelegramRidePublisher.class);
        rideService = new RideService(rideRepository, mock(ChatRepository.class), mock(MessageRepository.class),
                expoPushService, userService, rideMapper, mock(RideValidator.class), mongoTemplate, telegramRidePublisher);
        ReflectionTestUtils.setField(rideService, "activeRideTime", 30);

        when(userService.findByPhoneNumber(PHONE)).thenReturn(me);
        when(rideRepository.findByUserIdAndIsActiveOrderByDateDesc("me", true)).thenReturn(List.of());
        when(rideRepository.save(any(Ride.class))).thenAnswer(inv -> {
            Ride ride = inv.getArgument(0);
            ride.setId("new-ride");
            return ride;
        });
        when(rideMapper.rideToRideDto(any())).thenReturn(new RideResponse());
        when(userService.findUsersByIds(Set.of("partner"))).thenReturn(Map.of("partner", partner));
    }

    private static Place place(double x, double y) {
        return Place.builder().coordinates(new GeoJsonPoint(x, y)).distance(500).build();
    }

    private Ride candidate() {
        LocalDateTime now = LocalDateTime.now();
        return Ride.builder().id("partner-ride").userId("partner").isActive(true)
                .placeFrom(place(X, Y + HUNDRED_METERS)).placeTo(place(X + 0.1, Y + 0.1))
                .scheduledFrom(now.minusMinutes(5)).scheduledTo(now.plusMinutes(25))
                .build();
    }

    private void createRideMatching(Ride... existing) {
        when(rideMapper.rideDtoToRide(any())).thenReturn(Ride.builder()
                .placeFrom(place(X, Y)).placeTo(place(X + 0.1, Y + 0.1)).build());
        when(rideRepository.findByIsActiveAndScheduledToAfterAndPlaceFromCoordinatesNear(
                eq(true), any(LocalDateTime.class), any(GeoJsonPoint.class), any(Distance.class)))
                .thenReturn(List.of(existing));
        rideService.createRide(new RideRequest(), PHONE);
    }

    @Test
    void notifiesOwnerOfMatchingRideAndRecordsThePair() {
        createRideMatching(candidate());

        verify(expoPushService).send(eq("ExponentPushToken[p]"), eq("Знайдено попутника"), anyString(),
                eq(Map.of("type", "partner_found", "rideId", "partner-ride")));
        verify(mongoTemplate).updateMulti(any(Query.class), any(UpdateDefinition.class), eq(Ride.class));
    }

    @Test
    void doesNotNotifyTheSamePairTwice() {
        Ride alreadyNotified = candidate();
        alreadyNotified.setPartnerFoundNotifiedRideIds(Set.of("new-ride"));

        createRideMatching(alreadyNotified);

        verify(expoPushService, never()).send(any(), any(), any(), anyMap());
    }

    @Test
    void skipsRidesOutsideTheirOwnersWalkDistance() {
        Ride far = candidate();
        far.setPlaceFrom(place(X, Y + 6 * HUNDRED_METERS));

        createRideMatching(far);

        verify(expoPushService, never()).send(any(), any(), any(), anyMap());
    }

    @Test
    void skipsRidesWithNonOverlappingWindowsOrAlreadyAgreed() {
        Ride later = candidate();
        later.setScheduledFrom(LocalDateTime.now().plusHours(5));
        later.setScheduledTo(LocalDateTime.now().plusHours(6));
        Ride agreed = candidate();
        agreed.setAgreedPartnerUserId("someone");

        createRideMatching(later, agreed);

        verify(expoPushService, never()).send(any(), any(), any(), anyMap());
    }

    @Test
    void notificationFailureNeverBreaksRideCreation() {
        when(rideMapper.rideDtoToRide(any())).thenReturn(Ride.builder()
                .placeFrom(place(X, Y)).placeTo(place(X + 0.1, Y + 0.1)).build());
        doThrow(new RuntimeException("mongo down")).when(rideRepository)
                .findByIsActiveAndScheduledToAfterAndPlaceFromCoordinatesNear(any(), any(), any(), any());

        assertNotNull(rideService.createRide(new RideRequest(), PHONE));
    }

    @Test
    void sphericalDistanceReadsXAsLongitudeLikeMongo() {
        // 0.0009 degrees of latitude (y) is ~100 m on a 6378.1 km sphere.
        double meters = RideService.sphericalDistanceMeters(new Point(X, Y), new Point(X, Y + HUNDRED_METERS));
        assertEquals(100.2, meters, 0.5);
    }

    @Test
    void rideStatsQueriesAroundTheGivenPointInGeoJsonOrder() {
        when(mongoTemplate.count(any(Query.class), eq(Ride.class))).thenReturn(5L, 2L);

        var stats = rideService.getRideStats(Y, X, "me");

        assertEquals(3, stats.getActiveNow());
        assertEquals(2, stats.getScheduled());
        ArgumentCaptor<Query> query = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate, times(2)).count(query.capture(), eq(Ride.class));
        Document geoWithin = (Document) query.getAllValues().get(0).getQueryObject().get("placeFrom.coordinates");
        Sphere area = (Sphere) ((GeoCommand) geoWithin.get("$geoWithin")).getShape();
        // x = longitude, y = latitude.
        assertEquals(new Point(X, Y), area.getCenter());
    }

    @Test
    void activeRideErrorWorksForUserWithoutLastName() {
        me.setFirstName("Ivan");
        when(rideRepository.findByUserIdAndIsActiveOrderByDateDesc("me", true)).thenReturn(List.of(new Ride()));

        ValidationException e = assertThrows(ValidationException.class,
                () -> rideService.createRide(new RideRequest(), PHONE));

        assertEquals(ErrorCode.USER_HAS_ACTIVE_RIDE, e.getCode());
        assertEquals(Map.of("firstName", "Ivan", "lastName", ""), e.getParams());
    }

    @Test
    void newRideIsPublishedToTheTelegramChannel() {
        createRideMatching();

        ArgumentCaptor<Ride> published = ArgumentCaptor.forClass(Ride.class);
        verify(telegramRidePublisher).publishNewRide(published.capture());
        assertEquals("new-ride", published.getValue().getId());
    }

    @Test
    void cancelledRideMarksItsTelegramPost() {
        Ride ride = Ride.builder().id("r").isActive(true).telegramMessageId(42L).build();

        rideService.cancelRide(ride);

        verify(telegramRidePublisher).markRideEnded(ride);
        assertEquals(false, ride.getIsActive());
    }

    @Test
    void expiredRidesMarkTheirTelegramPosts() {
        Ride expired = Ride.builder().id("r").isActive(true).build();
        when(rideRepository.findByIsActiveTrueAndScheduledToBefore(any())).thenReturn(List.of(expired));

        rideService.deactivateOldRides();

        verify(telegramRidePublisher).markRideEnded(expired);
    }
}
