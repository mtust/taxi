package com.tustanovskyy.taxi.service.impl;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
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
        return rideRepository.save(ride);
    }

    @Override
    public Collection<Ride> findPartnersRide(Ride currentRide) {
        List<Ride> ridesFrom = rideRepository.findByPointFromNear(currentRide.getPointFrom().getX(), currentRide.getPointFrom().getY(), currentRide.getDistanceFrom());
        List<Ride> rideTo = rideRepository.findByPointToNear(currentRide.getPointTo().getX(), currentRide.getPointTo().getY(), currentRide.getDistanceTo());
        return ridesFrom
                .stream().filter(ride -> rideTo.contains(ride) && !ride.equals(currentRide)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Collection<Ride> findPartnersRide(String rideId) {
        Ride currentRide = rideRepository.findOne(new ObjectId(rideId));
        return this.findPartnersRide(currentRide);
    }
}
