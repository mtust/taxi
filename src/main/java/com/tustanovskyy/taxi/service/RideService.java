package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.repository.RideRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final UserService userService;
    private final RideMapper rideMapper;
    @Value("${taxi.ride.active.minutes}")
    private Integer activeRideTime;

    @Transactional
    public RideResponse createRide(RideRequest ride, String phoneNumber) {
        User user = userService.findByPhoneNumber(phoneNumber);
        String userId = user.getId();
        if (!rideRepository.findByUserIdAndIsActive(userId, true).isEmpty()) {
            log.error("user {} already have active rides", userId);
            throw new ValidationException("user " + user.getFirstName() + " " + user.getLastName() + " already have active rides. Please cancel active ride");
        }
        Ride rideDocument = rideMapper.rideDtoToRide(ride);
        rideDocument.setUserId(userId);
        rideDocument.setIsActive(true);
        log.info("ride to store: {}", rideDocument);
        return rideMapper.rideToRideDto(rideRepository.save(rideDocument));
    }


    @Transactional
    public Collection<RideDetails> findPartnersRide(Ride currentRide, boolean onlyFromPartner) {

        List<Ride> ridesFrom = findByPlaceFromCoordinatesNear(currentRide.getPlaceFrom());

        List<Ride> ridesTo = onlyFromPartner ? new ArrayList<>()
                : findByPlaceToCoordinatesNear(currentRide.getPlaceTo());

        return ridesFrom
                .stream()
                .filter(rideFrom -> onlyFromPartner || ridesTo.contains(rideFrom))
                .filter(rideFrom -> !rideFrom.getId().equals(currentRide.getId()))
                .map(ride -> rideMapper.rideToRideDetailsDto(ride, userService.findUser(ride.getUserId())))
                .collect(Collectors.toList());
    }


    @Transactional
    public Collection<RideDetails> findPartnersRide(String rideId, boolean onlyFromPartner) {
        Ride currentRide = this.getRide(rideId);
        log.info("ride: {}", currentRide);
        return this.findPartnersRide(currentRide, onlyFromPartner);
    }


    @Transactional
    public RideDetails findRide(String rideId) {
        return rideRepository.findById(new ObjectId(rideId))
                .map(ride -> rideMapper.rideToRideDetailsDto(ride, userService.findUser(ride.getUserId())))
                .orElseThrow(() -> new ValidationException("ride " + rideId + " not found"));
    }

    public Collection<RideResponse> findRidesByUserAndStatus(String userId, Boolean isActive) {
        return rideMapper.ridesToRideDto(rideRepository
                .findByUserIdAndIsActive(userId, isActive));
    }

    public void cancelRide(String id) {
        var ride = getRide(id);
        ride.setIsActive(false);
        rideRepository.save(ride);
    }

    private Ride getRide(String id) {
        return rideRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new ValidationException("ride " + id + " not found"));
    }

    private List<Ride> findByPlaceToCoordinatesNear(Place place) {
        return rideRepository.findByIsActiveAndDateAfterAndPlaceToCoordinatesNear(
                true,
                LocalDateTime.now().minusMinutes(activeRideTime),
                new GeoJsonPoint(place.getCoordinates().getX(),
                        place.getCoordinates().getY()),
                new Distance((double) place.getDistance() / 1000,
                        Metrics.KILOMETERS));
    }

    private List<Ride> findByPlaceFromCoordinatesNear(Place place) {
        return rideRepository.findByIsActiveAndDateAfterAndPlaceFromCoordinatesNear(
                true,
                LocalDateTime.now().minusMinutes(activeRideTime),
                new GeoJsonPoint(place.getCoordinates().getX(),
                        place.getCoordinates().getY()),
                new Distance((double) place.getDistance() / 1000,
                        Metrics.KILOMETERS));
    }

    private List<Ride> findByPlaceCoordinatesToNear(Place place) {
        return rideRepository.findActiveByToCoordinatesNear(true,
                LocalDateTime.now().minusMinutes(activeRideTime),
                place.getCoordinates(),
                (double) place.getDistance());
    }

    private List<Ride> findByPlaceCoordinatesFromNear(Place place) {
        return rideRepository.findActiveByFromCoordinatesNear(true,
                LocalDateTime.now().minusMinutes(activeRideTime),
                place.getCoordinates(),
                (double) place.getDistance());
    }
}
