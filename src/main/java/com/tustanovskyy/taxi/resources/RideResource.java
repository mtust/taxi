package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping(path = "ride")
@CrossOrigin(origins = "*")
public class RideResource {

    @Autowired
    RideService rideService;

    @Autowired
    UserService userService;

    @PostMapping
    public Ride createRide(@RequestBody Ride ride) {
        return rideService.createRide(ride);
    }


    @GetMapping("{id}")
    public Collection<Ride> findPartners(@PathVariable String id) {
        return rideService.findPartnersRide(id);
    }

    @RequestMapping(path = "partner", method = RequestMethod.GET)
    public Collection<User> findPartners(@RequestBody User user) {
        return userService.findPartners(user);
    }

}
