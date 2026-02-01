package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.domain.RideDetails;
import com.tustanovskyy.taxi.domain.Role;
import com.tustanovskyy.taxi.domain.request.ChatRequest;
import com.tustanovskyy.taxi.domain.request.RideRequest;
import com.tustanovskyy.taxi.domain.response.RideResponse;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.service.ChatService;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Collection;

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
                                                @RequestParam("onlyFromPartner") boolean onlyFromPartner) {
        log.info("id: {}", id);
        return rideService.findPartnersRide(id, onlyFromPartner);
    }

    @GetMapping
    public Collection<RideResponse> ridesByUser(@RequestParam("userId") String userId,
                                                @RequestParam("isActive") Boolean isActive,
                                                @AuthenticationPrincipal String phoneNumber) {
        var currentUser = userService.getUserByPhoneNumber(phoneNumber);
        if (!currentUser.getId().equals(userId) || Role.ADMIN.equals(currentUser.getRole())) {
            throw new ValidationException("Access denied");
        }
        return rideService.findRidesByUserAndStatus(userId, isActive);
    }

    @DeleteMapping("/{id}")
    public void cancelRide(@PathVariable String id) {
        rideService.cancelRide(id);
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
