package com.tustanovskyy.taxi.service.notification;

import java.util.Map;

/**
 * Localized push notification text for ride lifecycle events (propose/accept/complete/cancel),
 * keyed by User#language. Propose/accept deliberately reuse the same wording already shipped on
 * the FE for the equivalent badges (partners.wantsToAgreeBadge / chat.rideAgreed in
 * src/locales/*.json) so the notification reads as a continuation of UI the user has already
 * seen, not a separately-invented phrasing; complete/cancel follow the same short verb-phrase
 * style. Falls back to English for any unrecognized language code. "New message" pushes don't
 * need this - the title is just the sender's name and the body is their raw message content.
 */
public final class PushMessages {

    private PushMessages() {
    }

    private static final Map<String, String> RIDE_PROPOSE_BODY = Map.ofEntries(
            Map.entry("en", "Wants to agree on this ride"),
            Map.entry("uk", "Пропонує узгодити поїздку"),
            Map.entry("pl", "Chce uzgodnić ten przejazd"),
            Map.entry("es", "Quiere acordar este viaje"),
            Map.entry("de", "Möchte diese Fahrt vereinbaren"),
            Map.entry("fr", "Souhaite convenir de ce trajet"),
            Map.entry("th", "ต้องการยืนยันการเดินทางนี้"),
            Map.entry("ar", "يريد الموافقة على هذه الرحلة")
    );

    private static final Map<String, String> RIDE_ACCEPTED_BODY = Map.ofEntries(
            Map.entry("en", "Ride agreed"),
            Map.entry("uk", "Поїздку узгоджено"),
            Map.entry("pl", "Przejazd uzgodniony"),
            Map.entry("es", "Viaje acordado"),
            Map.entry("de", "Fahrt vereinbart"),
            Map.entry("fr", "Trajet convenu"),
            Map.entry("th", "ยืนยันการเดินทางแล้ว"),
            Map.entry("ar", "تمت الموافقة على الرحلة")
    );

    private static final Map<String, String> RIDE_COMPLETED_BODY = Map.ofEntries(
            Map.entry("en", "Ride completed"),
            Map.entry("uk", "Поїздку завершено"),
            Map.entry("pl", "Przejazd zakończony"),
            Map.entry("es", "Viaje finalizado"),
            Map.entry("de", "Fahrt abgeschlossen"),
            Map.entry("fr", "Trajet terminé"),
            Map.entry("th", "การเดินทางเสร็จสิ้น"),
            Map.entry("ar", "اكتملت الرحلة")
    );

    private static final Map<String, String> RIDE_CANCELLED_BODY = Map.ofEntries(
            Map.entry("en", "Ride cancelled"),
            Map.entry("uk", "Поїздку скасовано"),
            Map.entry("pl", "Przejazd anulowany"),
            Map.entry("es", "Viaje cancelado"),
            Map.entry("de", "Fahrt storniert"),
            Map.entry("fr", "Trajet annulé"),
            Map.entry("th", "ยกเลิกการเดินทางแล้ว"),
            Map.entry("ar", "تم إلغاء الرحلة")
    );

    public static String rideProposeBody(String language) {
        // Map.ofEntries() is an immutable map that throws NPE on a null key (unlike HashMap,
        // which handles null keys fine) - User#language is null for accounts that never
        // explicitly set one, so the lookup key has to be normalized before it ever reaches the
        // map, not handled via getOrDefault's (non-existent, for this map type) null handling.
        return RIDE_PROPOSE_BODY.getOrDefault(language != null ? language : "en", RIDE_PROPOSE_BODY.get("en"));
    }

    public static String rideAcceptedBody(String language) {
        return RIDE_ACCEPTED_BODY.getOrDefault(language != null ? language : "en", RIDE_ACCEPTED_BODY.get("en"));
    }

    public static String rideCompletedBody(String language) {
        return RIDE_COMPLETED_BODY.getOrDefault(language != null ? language : "en", RIDE_COMPLETED_BODY.get("en"));
    }

    public static String rideCancelledBody(String language) {
        return RIDE_CANCELLED_BODY.getOrDefault(language != null ? language : "en", RIDE_CANCELLED_BODY.get("en"));
    }
}
