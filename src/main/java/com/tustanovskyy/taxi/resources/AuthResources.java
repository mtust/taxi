package com.tustanovskyy.taxi.resources;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.CodeRequest;
import com.tustanovskyy.taxi.domain.request.LoginRequest;
import com.tustanovskyy.taxi.domain.request.PhoneRequest;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.domain.response.LoginResponse;
import com.tustanovskyy.taxi.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    public User signUp(@RequestBody SignUpRequest user, HttpServletRequest request) {
        return userService.createUser(user, clientIp(request));
    }

    @PostMapping("/verify/phone")
    public void resendVerificationCode(@RequestBody PhoneRequest phoneRequest, HttpServletRequest request) {
        userService.resendVerificationCode(phoneRequest.getPhoneNumber(), clientIp(request));
    }

    @PostMapping("/verify")
    public LoginResponse verifyCode(@RequestBody CodeRequest codeRequest) {
        return userService.validateCode(codeRequest.getCode(), codeRequest.getPhoneNumber());
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        return userService.login(loginRequest.getPhoneNumber(), loginRequest.getPassword(), clientIp(request));
    }

    @PostMapping("/forgot/password")
    public boolean forgotPassword(@RequestBody PhoneRequest phoneRequest, HttpServletRequest request) {
        return userService.forgotPassword(phoneRequest.getPhoneNumber(), clientIp(request));
    }

    @PostMapping("/recovery/password")
    public LoginResponse forgotPassword(@RequestBody RecoveryPasswordRequest recoveryPasswordRequest) {
        return userService.recoveryPassword(recoveryPasswordRequest);
    }

    /**
     * The caller's address, for per-IP rate limiting of verification codes. On Cloud Run the
     * socket peer is Google's front end, which appends the real client address as the LAST entry
     * of X-Forwarded-For - anything before it was supplied by the client and can't be trusted.
     */
    private static String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            String[] hops = forwardedFor.split(",");
            return hops[hops.length - 1].trim();
        }
        return request.getRemoteAddr();
    }
}
