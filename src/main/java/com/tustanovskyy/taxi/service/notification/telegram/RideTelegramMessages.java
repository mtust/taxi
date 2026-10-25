package com.tustanovskyy.taxi.service.notification.telegram;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Text of the rides-channel posts. Privacy matters here - the channel is public - so a post never
 * contains names, phone numbers or exact addresses: places are cut down to the street/landmark and
 * city (house numbers and postcodes dropped), see {@link #approximatePlace}.
 */
public final class RideTelegramMessages {

    private RideTelegramMessages() {
    }

    static final String JOIN_BUTTON = "Приєднатися";

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("dd.MM");
    /** Address parts that aren't a place or a city (house number, postcode, region). */
    private static final Pattern NOT_A_PLACE = Pattern.compile(".*\\d.*|.*(область|oblast|region|район|raion).*",
            Pattern.CASE_INSENSITIVE);

    /** Street/landmark and city of a place, e.g. "вулиця Хрещатик, 22, Київ, Україна, 02000" -> ("вулиця Хрещатик", "Київ"). */
    record ApproximatePlace(String place, String city) {
        String label() {
            return place == null || place.equals(city) ? city : place;
        }
    }

    static ApproximatePlace approximatePlace(Place place) {
        if (place == null || place.getName() == null || place.getName().isBlank()) {
            return new ApproximatePlace(null, null);
        }
        List<String> parts = new ArrayList<>(Arrays.stream(place.getName().split(","))
                .map(String::trim)
                .filter(part -> !part.isEmpty())
                .toList());
        // Google-formatted addresses always end with the country - drop it (before filtering
        // numbers, since the postcode can come after it).
        parts.removeIf(part -> part.matches("\\d[\\d\\s-]*"));
        if (parts.size() > 1) {
            parts.removeLast();
        }
        List<String> named = parts.stream().filter(part -> !NOT_A_PLACE.matcher(part).matches()).toList();
        if (named.isEmpty()) {
            return new ApproximatePlace(null, null);
        }
        return new ApproximatePlace(named.getFirst(), named.getLast());
    }

    /** "#Київ", "#Біла_Церква" - Telegram hashtags can't contain spaces or dashes. */
    static String cityHashtag(String city) {
        return city == null ? null : "#" + city.trim().replaceAll("[\\s-]+", "_").replaceAll("[^\\p{L}\\p{N}_]", "");
    }

    /**
     * "зараз" for an immediate ride, otherwise the window in {@code zone}, e.g. "сьогодні 18:00–18:30".
     * Ride times are stored as server-local LocalDateTime (Cloud Run runs in UTC).
     */
    static String timeLabel(Ride ride, ZoneId zone, LocalDate today) {
        if (!Boolean.TRUE.equals(ride.getIsScheduled()) || ride.getScheduledFrom() == null || ride.getScheduledTo() == null) {
            return "зараз";
        }
        ZonedDateTime from = toZone(ride.getScheduledFrom(), zone);
        ZonedDateTime to = toZone(ride.getScheduledTo(), zone);
        LocalDate day = from.toLocalDate();
        String dayLabel = day.equals(today) ? "сьогодні"
                : day.equals(today.plusDays(1)) ? "завтра"
                : DAY.format(day);
        return dayLabel + " " + TIME.format(from) + "–" + TIME.format(to);
    }

    private static ZonedDateTime toZone(LocalDateTime serverTime, ZoneId zone) {
        return serverTime.atZone(ZoneId.systemDefault()).withZoneSameInstant(zone);
    }

    private static final String CALL_TO_ACTION = "Поїхали разом і розділимо вартість таксі в HurtomTaxi 👇";

    static String newRide(Ride ride, ZoneId zone, LocalDate today) {
        return body(ride, zone, today) + "\n\n" + CALL_TO_ACTION;
    }

    /** The post once the ride is over - a completed ride with a partner reads as a success. */
    static String endedRide(Ride ride, ZoneId zone, LocalDate today) {
        boolean partnerFound = Boolean.TRUE.equals(ride.getIsCompleted()) && ride.getAgreedPartnerUserId() != null;
        String status = partnerFound ? "✅ Попутника знайдено" : "⛔ Вже неактуально";
        return status + "\n\n" + body(ride, zone, today);
    }

    private static String body(Ride ride, ZoneId zone, LocalDate today) {
        ApproximatePlace from = approximatePlace(ride.getPlaceFrom());
        ApproximatePlace to = approximatePlace(ride.getPlaceTo());
        StringBuilder text = new StringBuilder();
        String hashtag = cityHashtag(from.city() != null ? from.city() : to.city());
        if (hashtag != null) {
            text.append(hashtag).append("\n");
        }
        return text.append("🚕 Шукаю попутника\n")
                .append("📍 ").append(orUnknown(from.label())).append(" → ").append(orUnknown(to.label())).append("\n")
                .append("🕒 ").append(timeLabel(ride, zone, today))
                .toString();
    }

    private static String orUnknown(String label) {
        return label != null ? label : "?";
    }
}
