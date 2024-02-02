package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.RideDetailsDto;
import com.tustanovskyy.taxi.dto.RideDto;

import java.util.Collection;

public interface RideService {

    RideDto createRide(RideDto ride);

    Collection<RideDto> findPartnersRide(String rideId);

    Collection<RideDto> getRides();

    RideDetailsDto findRide(String rideId);
    Collection<RideDto> findPartnersRide(Ride ride);

    Collection<RideDto> findRidesByUserAndStatus(String userId, Boolean isActive);

    void cancelRide(String id);
}
