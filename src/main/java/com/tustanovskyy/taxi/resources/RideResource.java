package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import com.tustanovskyy.taxi.service.RideService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping(path = "rides")
@CrossOrigin(origins = "*")
@Slf4j
@AllArgsConstructor
public class RideResource {

    private final RideService rideService;

    @PostMapping
    public RideResponse createRide(@RequestBody RideRequest ride) {
        return rideService.createRide(ride);
    }

    @GetMapping("{id}")
    public RideDetails ride(@PathVariable String id) {
        log.info("id: {}", id);
        return rideService.findRide(id);
    }

    @GetMapping("/{id}/partners")
    public Collection<RideDetails> findPartners(@PathVariable String id) {
        log.info("id: {}", id);
        return rideService.findPartnersRide(id);
    }

    @GetMapping
    public Collection<RideResponse> ridesByUser(@RequestParam("userId") String userId,
                                                @RequestParam("isActive") Boolean isActive) {
        return rideService.findRidesByUserAndStatus(userId, isActive);
    }

    @DeleteMapping("/{id}")
    public void cancelRide(@PathVariable String id) {
        rideService.cancelRide(id);
    }

}
