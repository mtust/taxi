package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.domain.response.LoginResponse;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.mapper.UserMapper;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.security.JwtTokenUtil;
import com.tustanovskyy.taxi.service.validatior.UserValidator;
import java.time.LocalDateTime;
import java.util.Random;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserValidator userValidator;

    public User createUser(SignUpRequest user) {
        userValidator.validateSignUpRequest(user);
        var created = userRepository.save(userMapper.signUpRequestToUser(user)
                .setPassword(passwordEncoder.encode(user.getPassword())));
        sendVerificationCode(created);
        return created;
    }

    public User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));
    }

    public void sendVerificationCode(String phoneNumber) {
        sendVerificationCode(getUserByPhoneNumber(phoneNumber));
    }

    public LoginResponse validateCode(String code, String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        userValidator.validateVerificationCode(user, code);

        if (!user.isRegistrationCompleted()) {
            user.setRegistrationCompleted(true);
            user = userRepository.save(user);
        }

        return createLoginResponse(user);
    }

    public LoginResponse login(String phoneNumber, String password) {
        User user = getUserByPhoneNumber(phoneNumber);
        userValidator.validateLogin(user, password, passwordEncoder);
        return createLoginResponse(user);
    }

    public LoginResponse recoveryPassword(RecoveryPasswordRequest request) {
        userValidator.validateRecoveryPasswordRequest(request);
        User user = getUserByPhoneNumber(request.getPhoneNumber());
        userValidator.validateRecoveryPassword(user, request);

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordForgot(false);
        return createLoginResponse(userRepository.save(user));
    }

    public boolean forgotPassword(String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        user.setPasswordForgot(true);
        sendVerificationCode(user);
        return true;
    }

    private LoginResponse createLoginResponse(User user) {
        return new LoginResponse()
                .setToken(jwtTokenUtil.generateToken(user.getPhoneNumber()))
                .setUser(userMapper.toUserResponse(user));
    }

    public User getUserByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ValidationException("User not found"));
    }

    private void sendVerificationCode(User user) {
        String code = getRandomNumberString();
        user.setVerificationCodeDate(LocalDateTime.now());
        user.setVerificationCode(code);
        userRepository.save(user);
        smsService.sendSms(user.getPhoneNumber(), code);
    }

    private static String getRandomNumberString() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}

