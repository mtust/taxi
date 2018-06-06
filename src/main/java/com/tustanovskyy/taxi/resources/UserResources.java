package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;


@RestController
public class UserResources {

    @Autowired
    UserService userService;

    @Autowired
    RideService rideService;

    @PostMapping
    public User createUser(User user) {
        return userService.createUser(user);
    }

    @GetMapping({"userId/partner"})
    public Collection<User> findPartner(@PathVariable String userId) {
        return userService.findPartners(userId);

    }

}
