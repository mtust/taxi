package com.tustanovskyy.taxi.service.notification.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

class RideTelegramMessagesTest {

    private static final ZoneId KYIV = ZoneId.of("Europe/Kyiv");
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 25);

    private static Place place(String name) {
        return Place.builder().name(name).build();
    }

    /** Server-local time (what the Ride document stores) for a Kyiv wall-clock time. */
    private static LocalDateTime kyiv(int day, int hour, int minute) {
        return ZonedDateTime.of(2026, 10, day, hour, minute, 0, 0, KYIV)
                .withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static Ride ride(String from, String to) {
        return Ride.builder().placeFrom(place(from)).placeTo(place(to)).isScheduled(false).build();
    }

    @Test
    void dropsHouseNumberPostcodeAndCountry() {
        var place = RideTelegramMessages.approximatePlace(place("вулиця Хрещатик, 22, Київ, Україна, 02000"));
        assertEquals("вулиця Хрещатик", place.place());
        assertEquals("Київ", place.city());
    }

    @Test
    void dropsRegionAndKeepsLandmark() {
        var place = RideTelegramMessages.approximatePlace(place("Аеропорт Бориспіль, Бориспіль, Київська область, Україна"));
        assertEquals("Аеропорт Бориспіль", place.place());
        assertEquals("Бориспіль", place.city());
    }

    @Test
    void cityOnlyAddressIsShownAsTheCity() {
        var place = RideTelegramMessages.approximatePlace(place("Львів, Україна"));
        assertEquals("Львів", place.label());
        assertEquals("Львів", place.city());
    }

    @Test
    void streetsContainingNumbersFallBackToTheCity() {
        // "вулиця 1 Травня, 5" can't be shortened without risking a precise address - show the city.
        var place = RideTelegramMessages.approximatePlace(place("вулиця 1 Травня, 5, Київ, Україна"));
        assertEquals("Київ", place.label());
    }

    @Test
    void missingPlaceIsUnknown() {
        assertEquals(null, RideTelegramMessages.approximatePlace(null).label());
        assertEquals(null, RideTelegramMessages.approximatePlace(place(" ")).label());
    }

    @Test
    void hashtagsHaveNoSpacesOrDashes() {
        assertEquals("#Київ", RideTelegramMessages.cityHashtag("Київ"));
        assertEquals("#Біла_Церква", RideTelegramMessages.cityHashtag("Біла Церква"));
        assertEquals("#Івано_Франківськ", RideTelegramMessages.cityHashtag("Івано-Франківськ"));
    }

    @Test
    void immediateRideIsNow() {
        assertEquals("зараз", RideTelegramMessages.timeLabel(ride("a", "b"), KYIV, TODAY));
    }

    @Test
    void scheduledRideShowsDayAndWindowInKyivTime() {
        Ride today = ride("a", "b");
        today.setIsScheduled(true);
        today.setScheduledFrom(kyiv(25, 18, 0));
        today.setScheduledTo(kyiv(25, 18, 30));
        assertEquals("сьогодні 18:00–18:30", RideTelegramMessages.timeLabel(today, KYIV, TODAY));

        Ride tomorrow = ride("a", "b");
        tomorrow.setIsScheduled(true);
        tomorrow.setScheduledFrom(kyiv(26, 8, 15));
        tomorrow.setScheduledTo(kyiv(26, 9, 0));
        assertEquals("завтра 08:15–09:00", RideTelegramMessages.timeLabel(tomorrow, KYIV, TODAY));
    }

    @Test
    void newRidePostHasHashtagRouteTimeAndNoPrivateDetails() {
        Ride ride = ride("вулиця Хрещатик, 22, Київ, Україна, 02000", "Залізничний вокзал, Київ, Україна");
        ride.setUserId("user-123");

        String text = RideTelegramMessages.newRide(ride, KYIV, TODAY);

        assertEquals("""
                #Київ
                🚕 Шукаю попутника
                📍 вулиця Хрещатик → Залізничний вокзал
                🕒 зараз

                Поїхали разом і розділимо вартість таксі в HurtomTaxi 👇""", text);
        assertFalse(text.contains("22"));
        assertFalse(text.contains("user-123"));
    }

    @Test
    void endedPostSaysWhetherAPartnerWasFound() {
        Ride cancelled = ride("Київ, Україна", "Бровари, Україна");
        cancelled.setIsActive(false);
        assertTrue(RideTelegramMessages.endedRide(cancelled, KYIV, TODAY).startsWith("⛔ Вже неактуально\n\n#Київ"));

        Ride shared = ride("Київ, Україна", "Бровари, Україна");
        shared.setIsActive(false);
        shared.setIsCompleted(true);
        shared.setAgreedPartnerUserId("partner");
        String text = RideTelegramMessages.endedRide(shared, KYIV, TODAY);
        assertTrue(text.startsWith("✅ Попутника знайдено"));
        assertFalse(text.contains("HurtomTaxi 👇"), "no call to action once the ride is over");
    }
}
