package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;

import java.util.Collection;

public interface RideService {

    Ride createRide(Ride ride);

    Collection<Ride> findPartnersRide(Ride ride);

    Collection<Ride> findPartnersRide(String rideId);

    Collection<Ride> getRides();

    Ride findRide(String rideId);
}
