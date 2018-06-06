package com.tustanovskyy.taxi.service.impl;

import com.google.maps.model.Geometry;
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RideServiceImpl implements RideService {

    @Autowired
    RideRepository rideRepository;
    @Autowired
    UserRepository userRepository;

    @Override
    public Ride createRide(Ride ride) {
        ride.setPointFrom(createPointFromGeometry(ride.getPlaceFrom().getGeometry()));
        ride.setPointTo(createPointFromGeometry(ride.getPlaceTo().getGeometry()));
        return rideRepository.save(ride);
    }

    private Point createPointFromGeometry(Geometry geometry) {
        return new Point(geometry.location.lat, geometry.location.lng);
    }

    @Override
    public Collection<Ride> findPartnersRide(Ride currentRide) {
        List<Ride> ridesFrom = rideRepository.findByPointFromNear(currentRide.getPointFrom().getX(),
                        currentRide.getPointFrom().getY(),
                        currentRide.getDistanceFrom());
        List<Ride> ridesTo = rideRepository.findByPointToNear(currentRide.getPointTo().getX(),
                        currentRide.getPointTo().getY(),
                        currentRide.getDistanceTo());
        return ridesFrom
                .stream()
                .filter(rideFrom -> ridesTo.contains(rideFrom) && !rideFrom.equals(currentRide))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Collection<Ride> findPartnersRide(String rideId) {
        Ride currentRide = rideRepository.findOne(new ObjectId(rideId));
        return this.findPartnersRide(currentRide);
    }
}
