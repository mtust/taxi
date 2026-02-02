package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.request.ChatRequest;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import com.tustanovskyy.taxi.service.ChatService;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import java.util.Arrays;
import java.util.Collection;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "rides")
@CrossOrigin(origins = "*")
@Slf4j
@AllArgsConstructor
public class RideResource {

    private final RideService rideService;
    private final UserService userService;
    private final ChatService chatService;

    @PostMapping
    public RideResponse createRide(@RequestBody RideRequest ride,
                                   @AuthenticationPrincipal String phoneNumber) {
        log.info("Creating ride for user with phone number: {}", phoneNumber);
        return rideService.createRide(ride, phoneNumber);
    }

    @GetMapping("{id}")
    public RideDetails ride(@PathVariable String id) {
        log.info("id: {}", id);
        return rideService.findRide(id);
    }

    @GetMapping("/{id}/partners")
    public Collection<RideDetails> findPartners(@PathVariable String id,
                                                @RequestParam("onlyFromPartner") boolean onlyFromPartner,
                                                @AuthenticationPrincipal String phoneNumber) {
        log.info("id: {}", id);
        Ride currentRide = rideService.getRide(id);
        userService.checkAccess(phoneNumber, currentRide.getUserId());
        return rideService.findPartnersRide(currentRide, onlyFromPartner);
    }

    @GetMapping
    public Collection<RideResponse> ridesByUser(@RequestParam("userId") String userId,
                                                @RequestParam("isActive") Boolean isActive,
                                                @AuthenticationPrincipal String phoneNumber) {
        userService.checkAccess(phoneNumber, userId);
        return rideService.findRidesByUserAndStatus(userId, isActive);
    }

    @DeleteMapping("/{id}")
    public void cancelRide(@PathVariable String id, @AuthenticationPrincipal String phoneNumber) {
        Ride ride = rideService.getRide(id);
        userService.checkAccess(phoneNumber, ride.getUserId());
        rideService.cancelRide(ride);
    }

    @PostMapping("/{id}/chat/{partnerId}")
    public void createChatWithPartner(@PathVariable String id,
                                    @PathVariable String partnerId,
                                    @AuthenticationPrincipal String phoneNumber) {
        log.info("Creating chat for ride: {} with partner: {}", id, partnerId);
        var currentUser = userService.getUserByPhoneNumber(phoneNumber);
        var chatRequest = new ChatRequest(id, Arrays.asList(currentUser.getId(), partnerId));
        chatService.createChat(chatRequest, phoneNumber);
    }

}
