package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.Place;
import com.tustanovskyy.taxi.domain.request.EditUserRequest;
import com.tustanovskyy.taxi.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("users")
@Slf4j
@AllArgsConstructor
public class UserResources {


    private final UserService userService;

    @GetMapping("{userId}")
    public User findUser(@PathVariable String userId) {
        return userService.findUser(userId);
    }

    @PutMapping
    public User editUser(EditUserRequest editUserRequest, @AuthenticationPrincipal String phoneNumber) {
        return userService.editUser(editUserRequest, phoneNumber);
    }

    @DeleteMapping
    public void deleteUser(@AuthenticationPrincipal String phoneNumber) {
        userService.deleteUser(phoneNumber);
    }

    @PostMapping("home-address")
    public User addHomeAddress(Place homeAddress, @AuthenticationPrincipal String phoneNumber) {
        return userService.addHomeAddress(homeAddress, phoneNumber);
    }

}
