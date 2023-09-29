package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.dto.RideDto;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping(path = "rides")
@CrossOrigin(origins = "*")
@Slf4j
public class RideResource {

    @Autowired
    RideService rideService;

    @Autowired
    UserService userService;

    @PostMapping
    public RideDto createRide(@RequestBody RideDto ride) {
        return rideService.createRide(ride);
    }

    @GetMapping("{id}")
    public RideDto ride(@PathVariable String id) {
        log.info("id: " + id);
        return rideService.findRide(id);
    }

    @GetMapping("/partner/{id}")
    public Collection<RideDto> findPartners(@PathVariable String id) {
        log.info("id: " + id);
        return rideService.findPartnersRide(id);
    }

    @GetMapping
    public Collection<RideDto> rides() {
        return rideService.getRides();
    }

}
