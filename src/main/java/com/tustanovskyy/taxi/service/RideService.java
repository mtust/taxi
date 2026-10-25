package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.Sex;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import com.tustanovskyy.taxi.domain.response.RideStatsResponse;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.document.Message;
import com.tustanovskyy.taxi.repository.ChatRepository;
import com.tustanovskyy.taxi.repository.MessageRepository;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.service.notification.ExpoPushService;
import com.tustanovskyy.taxi.service.notification.PushMessages;
import com.tustanovskyy.taxi.service.notification.telegram.TelegramRidePublisher;
import com.tustanovskyy.taxi.service.validatior.RideValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;
    private final ExpoPushService expoPushService;
    private final UserService userService;
    private final RideMapper rideMapper;
    private final RideValidator rideValidator;
    private final MongoTemplate mongoTemplate;
    private final TelegramRidePublisher telegramRidePublisher;

    /** Radius Mongo uses for spherical geo queries ($near / $centerSphere), so distances computed here agree with it. */
    private static final double MONGO_EARTH_RADIUS_METERS = 6378100;
    /** Upper bound on any ride's walk distance (the FE caps it at 500 m) - only a pre-filter, see notifyPotentialPartners. */
    private static final int PARTNER_NOTIFY_PREFILTER_RADIUS_METERS = 1000;
    /** Roughly "the user's city" for the activity counts on the empty Partners screen. */
    private static final int RIDE_STATS_RADIUS_KM = 25;
    @Value("${taxi.ride.active.minutes}")
    private Integer activeRideTime;
    @Value("${taxi.ride.schedule.max-advance-hours}")
    private Integer maxScheduleAdvanceHours;
    @Value("${taxi.ride.schedule.max-window-minutes}")
    private Integer maxScheduleWindowMinutes;

    @Transactional
    public RideResponse createRide(RideRequest ride, String phoneNumber) {
        User user = userService.findByPhoneNumber(phoneNumber);
        String userId = user.getId();
        if (!rideRepository.findByUserIdAndIsActiveOrderByDateDesc(userId, true).isEmpty()) {
            log.error("user {} already have active rides", userId);
            throw new ValidationException(ErrorCode.USER_HAS_ACTIVE_RIDE,
                    "user " + user.displayName() + " already have active rides. Please cancel active ride",
                    // Map.of rejects nulls, and lastName is optional
                    Map.of("firstName", Objects.toString(user.getFirstName(), ""),
                            "lastName", Objects.toString(user.getLastName(), "")));
        }
        Ride rideDocument = rideMapper.rideDtoToRide(ride);
        rideDocument.setUserId(userId);
        rideDocument.setIsActive(true);

        LocalDateTime now = LocalDateTime.now();
        boolean scheduled = ride.getScheduledFrom() != null || ride.getScheduledTo() != null;
        if (scheduled) {
            rideValidator.validateSchedule(ride.getScheduledFrom(), ride.getScheduledTo(),
                    maxScheduleAdvanceHours, maxScheduleWindowMinutes);
            rideDocument.setScheduledFrom(ride.getScheduledFrom());
            rideDocument.setScheduledTo(ride.getScheduledTo());
            rideDocument.setIsScheduled(true);
        } else {
            rideDocument.setScheduledFrom(now);
            rideDocument.setScheduledTo(now.plusMinutes(activeRideTime));
            rideDocument.setIsScheduled(false);
        }
        log.info("ride to store: {}", rideDocument);
        Ride saved = rideRepository.save(rideDocument);
        notifyPotentialPartners(saved);
        telegramRidePublisher.publishNewRide(saved);
        return rideMapper.rideToRideDto(saved);
    }

    /**
     * Pushes a "partner found" notification to owners of existing active/scheduled rides that
     * the newly created ride would show up for in THEIR partner list (their walk distance, their
     * searchOnlyFrom, overlapping windows - mirroring findPartnersRide from their side), so they
     * don't have to keep the app open polling to find out someone matched. At most one push per
     * ride pair (tracked in Ride#partnerFoundNotifiedRideIds). Never allowed to fail ride creation.
     */
    private void notifyPotentialPartners(Ride newRide) {
        try {
            // Pre-filter with a generous radius, then apply each candidate's own walk distance -
            // a candidate with a wider radius than the new ride's would otherwise be missed.
            List<Ride> candidates = rideRepository.findByIsActiveAndScheduledToAfterAndPlaceFromCoordinatesNear(
                            true,
                            LocalDateTime.now(),
                            newRide.getPlaceFrom().getCoordinates(),
                            new Distance((double) PARTNER_NOTIFY_PREFILTER_RADIUS_METERS / 1000, Metrics.KILOMETERS))
                    .stream()
                    .filter(ride -> !ride.getUserId().equals(newRide.getUserId()))
                    .filter(ride -> ride.getAgreedPartnerUserId() == null)
                    .filter(ride -> ride.getPartnerFoundNotifiedRideIds() == null
                            || !ride.getPartnerFoundNotifiedRideIds().contains(newRide.getId()))
                    .filter(ride -> schedulesOverlap(ride, newRide))
                    .filter(ride -> isWithinWalkDistance(ride.getPlaceFrom(), newRide.getPlaceFrom()))
                    .filter(ride -> ride.isSearchOnlyFrom() || isWithinWalkDistance(ride.getPlaceTo(), newRide.getPlaceTo()))
                    .toList();
            if (candidates.isEmpty()) {
                return;
            }

            Map<String, User> usersById = userService.findUsersByIds(
                    candidates.stream().map(Ride::getUserId).collect(Collectors.toSet()));
            for (Ride candidate : candidates) {
                User owner = usersById.get(candidate.getUserId());
                if (owner == null) {
                    continue;
                }
                expoPushService.send(owner.getPushToken(),
                        PushMessages.partnerFoundTitle(owner.getLanguage()),
                        PushMessages.partnerFoundBody(owner.getLanguage()),
                        Map.of("type", "partner_found", "rideId", candidate.getId()));
            }

            // $addToSet rather than saving the loaded documents back, so a concurrent change to
            // one of these rides (e.g. an agree landing at the same moment) isn't overwritten.
            mongoTemplate.updateMulti(
                    Query.query(Criteria.where("_id").in(candidates.stream().map(Ride::getId).toList())),
                    new Update().addToSet("partnerFoundNotifiedRideIds", newRide.getId()),
                    Ride.class);
        } catch (Exception e) {
            log.warn("Failed to notify potential partners of ride {}: {}", newRide.getId(), e.getMessage());
        }
    }

    /**
     * Whether {@code other} lies within {@code own}'s walk distance - the same check the geo
     * queries in findPartnersRide apply from {@code own}'s side. Points are GeoJSON, i.e.
     * [x, y] = [lng, lat] (see the FE's utils/geo.ts), computed on the same sphere Mongo's
     * $near uses so this agrees exactly with what the candidate sees in their own partner list.
     */
    private boolean isWithinWalkDistance(Place own, Place other) {
        if (own == null || other == null || own.getDistance() == null
                || own.getCoordinates() == null || other.getCoordinates() == null) {
            return false;
        }
        return sphericalDistanceMeters(own.getCoordinates(), other.getCoordinates()) <= own.getDistance();
    }

    static double sphericalDistanceMeters(Point a, Point b) {
        double lat1 = Math.toRadians(a.getY());
        double lat2 = Math.toRadians(b.getY());
        double dLat = lat2 - lat1;
        double dLng = Math.toRadians(b.getX() - a.getX());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return 2 * MONGO_EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    /**
     * Real counts of other users' rides still searching for a partner within
     * {@link #RIDE_STATS_RADIUS_KM} of the given point. Only active rides whose window hasn't
     * ended count, and scheduled rides can't be more than max-advance-hours ahead, so this is
     * effectively "today/tomorrow" activity around the caller.
     */
    public RideStatsResponse getRideStats(double latitude, double longitude, String excludeUserId) {
        // GeoJSON order: x = longitude, y = latitude.
        Circle area = new Circle(new Point(longitude, latitude),
                RIDE_STATS_RADIUS_KM * 1000.0 / MONGO_EARTH_RADIUS_METERS);
        Criteria base = Criteria.where("isActive").is(true)
                .and("scheduledTo").gt(LocalDateTime.now())
                .and("placeFrom.coordinates").withinSphere(area);
        if (excludeUserId != null) {
            base = base.and("userId").ne(excludeUserId);
        }
        long total = mongoTemplate.count(Query.query(base), Ride.class);
        long scheduled = mongoTemplate.count(Query.query(Criteria.where("isScheduled").is(true)).addCriteria(base), Ride.class);
        return new RideStatsResponse(total - scheduled, scheduled, RIDE_STATS_RADIUS_KM);
    }

    @Transactional
    public Collection<RideDetails> findPartnersRide(Ride currentRide, boolean onlyFromPartner) {
        return findPartnersRide(currentRide, onlyFromPartner, false);
    }

    @Transactional
    public Collection<RideDetails> findPartnersRide(Ride currentRide, boolean onlyFromPartner, boolean sameSex) {

        if (onlyFromPartner != currentRide.isSearchOnlyFrom()) {
            currentRide.setSearchOnlyFrom(onlyFromPartner);
            rideRepository.save(currentRide);
        }

        List<Ride> ridesFrom = findByPlaceFromCoordinatesNear(currentRide.getPlaceFrom());

        List<Ride> ridesTo = onlyFromPartner ? new ArrayList<>()
                : findByPlaceToCoordinatesNear(currentRide.getPlaceTo());
        Set<String> ridesToIds = ridesTo.stream().map(Ride::getId).collect(Collectors.toSet());

        List<Ride> candidates = ridesFrom
                .stream()
                .filter(rideFrom -> onlyFromPartner || ridesToIds.contains(rideFrom.getId()))
                .filter(rideFrom -> !rideFrom.getId().equals(currentRide.getId()))
                .filter(rideFrom -> !sameSex || getRideSex(rideFrom).equals(getRideSex(currentRide)))
                .filter(rideFrom -> schedulesOverlap(rideFrom, currentRide))
                .toList();

        // Batch-fetch the candidates' users in one query instead of one findUser() call per
        // candidate - this endpoint is polled every few seconds while a ride is searching for
        // a partner, so an N+1 lookup here multiplies with the number of nearby candidate rides
        // on every single poll.
        Map<String, User> usersById = userService.findUsersByIds(
                candidates.stream().map(Ride::getUserId).collect(Collectors.toSet()));

        // Same reasoning: one batched lookup of all of the requester's active chats, checked
        // per-candidate in memory, instead of a chat-existence query per candidate. A Chat
        // document exists as soon as either side opens the chat screen (see resolveChatId on the
        // FE), before anyone has actually typed anything, so only count chats that have at least
        // one real (non-system) message - otherwise merely glancing at an empty chat would make
        // it look, to either side, like a conversation had already started.
        Set<String> chatPartnerIds = chatRepository.findByParticipantIdsContainingAndIsActive(currentRide.getUserId(), true)
                .stream()
                .filter(chat -> messageRepository.existsByChatIdAndTypeNot(chat.getId(), Message.MessageType.SYSTEM))
                .flatMap(chat -> chat.getParticipantIds().stream())
                .filter(id -> !id.equals(currentRide.getUserId()))
                .collect(Collectors.toSet());

        return candidates
                .stream()
                .map(ride -> {
                    RideDetails details = rideMapper.rideToRideDetailsDto(ride, usersById.get(ride.getUserId()));
                    details.setHasChatWithMe(chatPartnerIds.contains(ride.getUserId()));
                    return details;
                })
                .collect(Collectors.toList());
    }

    /**
     * Two rides are only realistic partners if the windows they're searching for a ride in
     * actually overlap - e.g. a ride scheduled for 21:00-22:00 shouldn't be matched with one
     * scheduled for 09:00-10:00 tomorrow. Rides without a window (shouldn't happen for anything
     * created after this feature shipped, but guards old data) are treated as always overlapping.
     */
    private boolean schedulesOverlap(Ride a, Ride b) {
        LocalDateTime aFrom = a.getScheduledFrom();
        LocalDateTime aTo = a.getScheduledTo();
        LocalDateTime bFrom = b.getScheduledFrom();
        LocalDateTime bTo = b.getScheduledTo();
        if (aFrom == null || aTo == null || bFrom == null || bTo == null) {
            return true;
        }
        return aFrom.isBefore(bTo) && bFrom.isBefore(aTo);
    }

    @Transactional
    public RideDetails findRide(String rideId) {
        return rideRepository.findById(new ObjectId(rideId))
                .map(ride -> rideMapper.rideToRideDetailsDto(ride, userService.findUser(ride.getUserId())))
                .orElseThrow(() -> new ValidationException(ErrorCode.RIDE_NOT_FOUND, "ride " + rideId + " not found",
                        Map.of("rideId", rideId)));
    }

    public Collection<RideResponse> findRidesByUserAndStatus(String userId, Boolean isActive, int page, int size) {
        return rideMapper.ridesToRideDto(rideRepository
                .findByUserIdAndIsActiveOrderByDateDesc(userId, isActive, PageRequest.of(page, size)));
    }

    public void cancelRide(Ride ride) {
        ride.setIsActive(false);
        rideRepository.save(ride);
        telegramRidePublisher.markRideEnded(ride);
    }

    @Scheduled(fixedRate = 60000)
    public void deactivateOldRides() {
        LocalDateTime now = LocalDateTime.now();
        List<Ride> oldRides = rideRepository.findByIsActiveTrueAndScheduledToBefore(now);
        if (!oldRides.isEmpty()) {
            log.info("Deactivating {} rides whose search window ended before {}", oldRides.size(), now);
            oldRides.forEach(ride -> ride.setIsActive(false));
            rideRepository.saveAll(oldRides);
            oldRides.forEach(telegramRidePublisher::markRideEnded);
        }
    }

    public Ride getRide(String id) {
        return rideRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new ValidationException(ErrorCode.RIDE_NOT_FOUND, "ride " + id + " not found",
                        Map.of("rideId", id)));
    }

    /**
     * A user has at most one active ride at a time (enforced in createRide), so the most
     * recent one from this ordered lookup is their current one - or null if they don't have one.
     */
    public Ride findActiveRideByUserId(String userId) {
        return rideRepository.findByUserIdAndIsActiveOrderByDateDesc(userId, true)
                .stream()
                .findFirst()
                .orElse(null);
    }

    private List<Ride> findByPlaceToCoordinatesNear(Place place) {
        return rideRepository.findByIsActiveAndScheduledToAfterAndPlaceToCoordinatesNear(
                true,
                LocalDateTime.now(),
                new GeoJsonPoint(place.getCoordinates().getX(),
                        place.getCoordinates().getY()),
                new Distance((double) place.getDistance() / 1000,
                        Metrics.KILOMETERS));
    }

    private List<Ride> findByPlaceFromCoordinatesNear(Place place) {
        return rideRepository.findByIsActiveAndScheduledToAfterAndPlaceFromCoordinatesNear(
                true,
                LocalDateTime.now(),
                new GeoJsonPoint(place.getCoordinates().getX(),
                        place.getCoordinates().getY()),
                new Distance((double) place.getDistance() / 1000,
                        Metrics.KILOMETERS));
    }

    private Sex getRideSex(Ride ride) {
        return userService.findUser(ride.getUserId()).getSex();
    }

    @Transactional
    public RideResponse agreeRide(String rideId, String partnerUserId, String phoneNumber) {
        if (partnerUserId == null || partnerUserId.isBlank()) {
            // Without this check, a null partnerUserId sails past every check below (a Mongo
            // query with a null field just matches nothing) until it hits findUser(null) in
            // notifyPartnerOfAgreement, which throws a raw, uninformative NPE deep in Spring
            // Data's id-conversion internals instead of a clean 400.
            throw new ValidationException(ErrorCode.PARTNER_USER_ID_REQUIRED, "Partner user id is required");
        }
        User user = userService.findByPhoneNumber(phoneNumber);
        Ride ride = getRide(rideId);
        if (!ride.getUserId().equals(user.getId())) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED, "Access denied");
        }

        // agreeRide is called by both sides of a proposal: the ride owner proposing to
        // partnerUserId (first call), and the invited partner accepting by calling this same
        // method on THEIR OWN ride with partnerUserId = the original proposer. Distinguish which
        // one this is - before overwriting - by checking whether the partner's ride already
        // points back at us: if so, this call just completed mutual agreement (an accept);
        // otherwise it's a fresh proposal.
        Ride partnerRide = findActiveRideByUserId(partnerUserId);
        boolean isAccept = partnerRide != null && user.getId().equals(partnerRide.getAgreedPartnerUserId());

        ride.setAgreedPartnerUserId(partnerUserId);
        RideResponse response = rideMapper.rideToRideDto(rideRepository.save(ride));

        notifyPartnerOfAgreement(partnerUserId, user, isAccept);
        return response;
    }

    private void notifyPartnerOfAgreement(String partnerUserId, User proposer, boolean isAccept) {
        User partner = userService.findUser(partnerUserId);
        String proposerName = proposer.displayName();
        String body = isAccept
                ? PushMessages.rideAcceptedBody(partner.getLanguage())
                : PushMessages.rideProposeBody(partner.getLanguage());
        expoPushService.send(partner.getPushToken(), proposerName, body, Map.of("type", "ride_agree"));
    }

    /**
     * Clears a pending agreement proposal. Two different people can legitimately call this on
     * the same ride: the ride's owner cancelling their own outgoing proposal, or the invited
     * partner (ride.agreedPartnerUserId points at them) declining it - so access is granted to
     * either, rather than checked against ownership alone like agreeRide.
     */
    @Transactional
    public RideResponse declineRide(String rideId, String phoneNumber) {
        User user = userService.findByPhoneNumber(phoneNumber);
        Ride ride = getRide(rideId);
        boolean isOwner = ride.getUserId().equals(user.getId());
        boolean isInvitedPartner = ride.getAgreedPartnerUserId() != null
                && ride.getAgreedPartnerUserId().equals(user.getId());
        if (!isOwner && !isInvitedPartner) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        ride.setAgreedPartnerUserId(null);
        return rideMapper.rideToRideDto(rideRepository.save(ride));
    }

    /**
     * Completes the caller's ride, and - if they had a mutually-agreed partner (both rides
     * point agreedPartnerUserId at each other) - completes the partner's ride too, so one
     * person ending a shared ride doesn't leave the other stuck showing it as still active.
     */
    @Transactional
    public void completeRide(String rideId, String phoneNumber) {
        User user = userService.findByPhoneNumber(phoneNumber);
        Ride ride = getRide(rideId);
        if (!ride.getUserId().equals(user.getId())) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        ride.setIsActive(false);
        ride.setIsCompleted(true);
        rideRepository.save(ride);
        telegramRidePublisher.markRideEnded(ride);

        if (ride.getAgreedPartnerUserId() != null) {
            Ride partnerRide = findActiveRideByUserId(ride.getAgreedPartnerUserId());
            boolean partnerMutuallyAgreed = partnerRide != null
                    && ride.getUserId().equals(partnerRide.getAgreedPartnerUserId());
            if (partnerMutuallyAgreed) {
                partnerRide.setIsActive(false);
                partnerRide.setIsCompleted(true);
                rideRepository.save(partnerRide);
                telegramRidePublisher.markRideEnded(partnerRide);
            }
        }
    }
}
