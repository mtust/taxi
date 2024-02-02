package com.tustanovskyy.taxi.service.impl;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.RideDetailsDto;
import com.tustanovskyy.taxi.dto.RideDto;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.mapstruct.factory.Mappers;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
public class RideServiceImpl implements RideService {

    private final RideMapper rideMapper = Mappers.getMapper(RideMapper.class);

    private final RideRepository rideRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RideDto createRide(RideDto ride) {
        log.info("ride to store: " + ride);
        Ride rideDocument = rideMapper.rideDtoToRide(ride);
        rideDocument.setIsActive(true);
        return rideMapper.rideToRideDto(rideRepository.save(rideDocument));
    }

    @Override
    public Collection<RideDto> findPartnersRide(Ride currentRide) {
        List<Ride> ridesFrom = rideRepository.findByPlaceFromCoordinatesNear(
                new GeoJsonPoint(currentRide.getPlaceFrom().getCoordinates().getX(), currentRide.getPlaceFrom().getCoordinates().getY()),
                new Distance((double) currentRide.getPlaceFrom().getDistance() / 1000, Metrics.KILOMETERS));
        log.info("ridesFrom: " + ridesFrom);
        List<Ride> ridesTo = rideRepository.findByPlaceToCoordinatesNear(
                new GeoJsonPoint(currentRide.getPlaceTo().getCoordinates().getX(), currentRide.getPlaceTo().getCoordinates().getY()),
                new Distance((double) currentRide.getPlaceTo().getDistance() / 1000, Metrics.KILOMETERS));
        log.info("ridesTo: " + ridesTo);
        return rideMapper.ridesToRideDtos(ridesFrom
                .stream()
                .filter(rideFrom -> ridesTo.contains(rideFrom) && !rideFrom.equals(currentRide))
                .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Collection<RideDto> findPartnersRide(String rideId) {
        Ride currentRide = rideRepository.findById(new ObjectId(rideId)).get();
        log.info("ride: " + currentRide);
        return this.findPartnersRide(currentRide);
    }

    @Override
    @Transactional
    public RideDetailsDto findRide(String rideId) {
        return rideRepository.findById(new ObjectId(rideId))
                .map(ride -> {
                    Optional<User> user = ride.getUserId() == null ? Optional.empty() : userRepository.findById(ride.getUserId());
                    return rideMapper.rideToRideDetailsDto(ride, user.orElse(null));
                }).orElseThrow(() -> new ValidationException("ride " + rideId + " not found"));
    }

    @Override
    public Collection<RideDto> getRides() {
        return rideMapper.ridesToRideDtos(rideRepository.findAll());
    }

    @Override
    public Collection<RideDto> findRidesByUserAndStatus(String userId, Boolean isActive) {
        return rideMapper.ridesToRideDtos(rideRepository
                .findByUserIdAndIsActive(userId, isActive));
    }

    @Override
    public void cancelRide(String id) {
        var ride = getRide(id);
        ride.setIsActive(false);
        rideRepository.save(ride);
    }

    private Ride getRide(String id) {
        return rideRepository.findById(new ObjectId(id))
                .orElseThrow(() -> new ValidationException("ride " + id + " not found"));
    }
}
