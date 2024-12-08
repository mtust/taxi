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
import com.tustanovskyy.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RideService {

    private final RideRepository rideRepository;
    private final UserRepository userRepository;
    private final RideMapper rideMapper;
    @Value("${taxi.ride.active.minutes}")
    private Integer activeRideTime;

    @Transactional
    public RideResponse createRide(RideRequest ride) {
        log.info("ride to store: {}", ride);
        Ride rideDocument = rideMapper.rideDtoToRide(ride);
        rideDocument.setIsActive(true);
        return rideMapper.rideToRideDto(rideRepository.save(rideDocument));
    }


    public Collection<RideResponse> findPartnersRide(Ride currentRide) {

        List<Ride> ridesFrom = findByPlaceFromCoordinatesNear(currentRide.getPlaceFrom());
        log.info("ridesFrom: {}", ridesFrom);

        List<Ride> ridesTo = findByPlaceToCoordinatesNear(currentRide.getPlaceTo());
        log.info("ridesTo: {}", ridesTo);

        return rideMapper.ridesToRideDto(ridesFrom
                .stream()
                .filter(rideFrom -> ridesTo.contains(rideFrom) && !rideFrom.equals(currentRide))
                .collect(Collectors.toList()));
    }


    @Transactional
    public Collection<RideResponse> findPartnersRide(String rideId) {
        Ride currentRide = rideRepository.findById(new ObjectId(rideId)).get();
        log.info("ride: {}", currentRide);
        return this.findPartnersRide(currentRide);
    }


    @Transactional
    public RideDetails findRide(String rideId) {
        return rideRepository.findById(new ObjectId(rideId))
                .map(ride -> {
                    Optional<User> user = ride.getUserId() == null ? Optional.empty() : userRepository.findById(ride.getUserId());
                    return rideMapper.rideToRideDetailsDto(ride, user.orElse(null));
                }).orElseThrow(() -> new ValidationException("ride " + rideId + " not found"));
    }


    public Collection<RideResponse> getRides() {
        return rideMapper.ridesToRideDto(rideRepository.findAll());
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
