package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.CodeDto;
import com.tustanovskyy.taxi.dto.PhoneDto;
import com.tustanovskyy.taxi.dto.UserDto;
import com.tustanovskyy.taxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("users")
@Slf4j
public class UserResources {


    private final UserService userService;

    public UserResources(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    public User createUser(@RequestBody UserDto user) {
        return userService.createUser(user);
    }

    @GetMapping("{userId}")
    public User findUser(@PathVariable String userId) {
        return userService.findUser(userId);
    }

    @PostMapping("verify/phone")
    public void verifyPhone(@RequestBody PhoneDto phoneDto) {
            userService.sendVerificationCode(phoneDto.getPhoneNumber());
    }

    @PostMapping("verify")
    public void verifyCode(@RequestBody CodeDto codeDto) {
        userService.validateCode(codeDto.getCode(), codeDto.getPhoneNumber());
    }

}
