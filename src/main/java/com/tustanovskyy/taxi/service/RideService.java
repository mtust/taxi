package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.Sex;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.service.validatior.RideValidator;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final UserService userService;
    private final RideMapper rideMapper;
    private final RideValidator rideValidator;
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
                    "user " + user.getFirstName() + " " + user.getLastName() + " already have active rides. Please cancel active ride",
                    Map.of("firstName", user.getFirstName(), "lastName", user.getLastName()));
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
        return rideMapper.rideToRideDto(rideRepository.save(rideDocument));
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

        return candidates
                .stream()
                .map(ride -> rideMapper.rideToRideDetailsDto(ride, usersById.get(ride.getUserId())))
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
    }

    @Scheduled(fixedRate = 60000)
    public void deactivateOldRides() {
        LocalDateTime now = LocalDateTime.now();
        List<Ride> oldRides = rideRepository.findByIsActiveTrueAndScheduledToBefore(now);
        if (!oldRides.isEmpty()) {
            log.info("Deactivating {} rides whose search window ended before {}", oldRides.size(), now);
            oldRides.forEach(ride -> ride.setIsActive(false));
            rideRepository.saveAll(oldRides);
        }
    }

    public Ride getRide(String id) {
        return rideRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new ValidationException(ErrorCode.RIDE_NOT_FOUND, "ride " + id + " not found",
                        Map.of("rideId", id)));
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
        User user = userService.findByPhoneNumber(phoneNumber);
        Ride ride = getRide(rideId);
        if (!ride.getUserId().equals(user.getId())) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        ride.setAgreedPartnerUserId(partnerUserId);
        return rideMapper.rideToRideDto(rideRepository.save(ride));
    }

    /**
     * Clears a pending agreement proposal. The caller isn't the ride's owner here - they're the
     * partner the owner proposed to (ride.agreedPartnerUserId points at them) - so access is
     * checked against that field instead of ownership, matching agreeRide's ownership check.
     */
    @Transactional
    public RideResponse declineRide(String rideId, String phoneNumber) {
        User user = userService.findByPhoneNumber(phoneNumber);
        Ride ride = getRide(rideId);
        if (ride.getAgreedPartnerUserId() == null || !ride.getAgreedPartnerUserId().equals(user.getId())) {
            throw new ValidationException(ErrorCode.ACCESS_DENIED, "Access denied");
        }
        ride.setAgreedPartnerUserId(null);
        return rideMapper.rideToRideDto(rideRepository.save(ride));
    }

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
    }
}
