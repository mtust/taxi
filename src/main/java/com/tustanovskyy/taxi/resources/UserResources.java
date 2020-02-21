package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("users")
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

//    @GetMapping
//    public Collection<User> findPartner(@RequestParam String userId) {
//        return userService.findPartners(userId);
//    }

}
