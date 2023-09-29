package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.RideDto;

import java.util.Collection;

public interface RideService {

    RideDto createRide(RideDto ride);

    Collection<RideDto> findPartnersRide(String rideId);

    Collection<RideDto> getRides();

    RideDto findRide(String rideId);
    Collection<RideDto> findPartnersRide(Ride ride);
}
