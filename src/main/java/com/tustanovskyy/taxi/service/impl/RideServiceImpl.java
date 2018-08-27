package com.tustanovskyy.taxi.service.impl;

import com.google.maps.model.Geometry;
import com.tustanovskyy.taxi.document.Place;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RideServiceImpl implements RideService {

    @Autowired
    RideRepository rideRepository;
    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public Ride createRide(Ride ride) {
//        ride.setPointFrom(createPointFromGeometry(ride.getPlaceFrom().getGeometry()));
//        ride.setPointTo(createPointFromGeometry(ride.getPlaceTo().getGeometry()));
        ride.setIsActive(true);
        return rideRepository.save(ride);
    }

    private Point createPointFromGeometry(Geometry geometry) {
        return new Point(geometry.location.lat, geometry.location.lng);
    }

    @Override
    public Collection<Ride> findPartnersRide(Ride currentRide) {
        Place placeFrom = currentRide.getPlaceFrom();
        List<Ride> ridesFrom = rideRepository.findByPlaceFrom_PointNear(placeFrom.getPoint().getX(),
                        placeFrom.getPoint().getY(),
                        placeFrom.getDistance());
        log.info("ridesFrom: " + ridesFrom);
        Place placeTo = currentRide.getPlaceTo();
        List<Ride> ridesTo = rideRepository.findByPlaceTo_PointNear(placeTo.getPoint().getX(),
                        placeTo.getPoint().getY(),
                        placeTo.getDistance());
        log.info("ridesTo: " + ridesTo);
        return ridesFrom
                .stream()
                .filter(rideFrom -> ridesTo.contains(rideFrom) && !rideFrom.equals(currentRide))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Collection<Ride> findPartnersRide(String rideId) {
        Ride currentRide = rideRepository.findOne(new ObjectId(rideId));
        log.info("ride: " + currentRide);
        return this.findPartnersRide(currentRide);
    }

    @Override
    @Transactional
    public Ride findRide(String rideId) {
        return rideRepository.findOne(new ObjectId(rideId));
    }

    @Override
    public Collection<Ride> getRides() {
        return rideRepository.findAll();
    }
}
