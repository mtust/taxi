package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.CodeRequest;
import com.tustanovskyy.taxi.domain.request.LoginRequest;
import com.tustanovskyy.taxi.domain.request.PhoneRequest;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.domain.response.LoginResponse;
import com.tustanovskyy.taxi.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.repository.query.Param;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("auth")
@Slf4j
@AllArgsConstructor
public class AuthResources {

    private final UserService userService;

    @PostMapping("/signup")
    public User signUp(@RequestBody SignUpRequest user) {
        return userService.createUser(user);
    }

    @PostMapping("verify/phone")
    public void verifyPhone(@RequestBody PhoneRequest phoneRequest) {
        userService.sendVerificationCode(phoneRequest.getPhoneNumber());
    }

    @PostMapping("/verify")
    public LoginResponse verifyCode(@RequestBody CodeRequest codeRequest) {
        return userService.validateCode(codeRequest.getCode(), codeRequest.getPhoneNumber());
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest) {
        return userService.login(loginRequest.getPhoneNumber(), loginRequest.getPassword());
    }

    @PostMapping("/forgot/password")
    public boolean forgotPassword(@Param("phoneNumber") String phoneNumber) {
        return userService.forgotPassword(phoneNumber);
    }

    @PostMapping("/recovery/password")
    public LoginResponse forgotPassword(@RequestBody RecoveryPasswordRequest recoveryPasswordRequest) {
        return userService.recoveryPassword(recoveryPasswordRequest);
    }
}
