package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.CodeRequest;
import com.tustanovskyy.taxi.domain.request.PhoneRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;


@RestController
@CrossOrigin(origins = "*")
@RequestMapping("users")
@Slf4j
@AllArgsConstructor
public class UserResources {


    private final UserService userService;

    @PostMapping
    public User signUp(@RequestBody SignUpRequest user) {
        return userService.createUser(user);
    }

    @GetMapping("{userId}")
    public User findUser(@PathVariable String userId) {
        return userService.findUser(userId);
    }

    @PostMapping("verify/phone")
    public void verifyPhone(@RequestBody PhoneRequest phoneRequest) {
            userService.sendVerificationCode(phoneRequest.getPhoneNumber());
    }

    @PostMapping("verify")
    public User verifyCode(@RequestBody CodeRequest codeRequest) {
        return userService.validateCode(codeRequest.getCode(), codeRequest.getPhoneNumber());
    }

}
