package com.tustanovskyy.taxi.service.impl;

import com.google.maps.model.Geometry;
import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.dto.RideDto;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.mapstruct.factory.Mappers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.mongodb.core.geo.GeoJsonPoint;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RideServiceImpl implements RideService {

    RideMapper rideMapper = Mappers.getMapper(RideMapper.class);

    @Autowired
    RideRepository rideRepository;
    @Autowired
    UserRepository userRepository;

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
                new GeoJsonPoint(currentRide.getPlaceTo().getCoordinates().getX(), currentRide.getPlaceFrom().getCoordinates().getY()),
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
    public RideDto findRide(String rideId) {
        return rideMapper.rideToRideDto(rideRepository.findById(new ObjectId(rideId)).get());
    }

    @Override
    public Collection<RideDto> getRides() {
        return rideMapper.ridesToRideDtos(rideRepository.findAll());
    }
}
