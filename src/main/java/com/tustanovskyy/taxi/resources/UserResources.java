package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.exception.VerificationCodeException;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("users")
@Slf4j
public class UserResources {

    @Autowired
    UserService userService;

    @Autowired
    RideService rideService;

    @PostMapping
    public User createUser(@RequestBody User user) {
        return userService.createUser(user);
    }

    @GetMapping("{userId}")
    public User findUser(@PathVariable Long userId) {
        return userService.findUser(userId);
    }

    @GetMapping("facebook/{facebookId}")
    public User findUserByFacebookId(@PathVariable String facebookId) {
        return userService.findUserByFacebookId(facebookId);
    }

    @PostMapping("verify/phone")
    public void verifyPhone(@RequestBody Map<String, String> phoneNumber) {
        if (phoneNumber != null) {
            userService.sendVerificationCode(phoneNumber.get("phoneNumber"));
        }
    }

    @PostMapping("verify/code")
    public void verifyCode(@RequestBody Map<String, String> request) {
        if (request != null) {
            userService.validateCode(request.get("code"), request.get("phoneNumber"));
        } else {
            throw new VerificationCodeException("code not exist");
        }
    }

//    @GetMapping
//    public Collection<User> findPartner(@RequestParam String userId) {
//        return userService.findPartners(userId);
//    }

}
