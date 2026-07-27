package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.Place;
import com.tustanovskyy.taxi.domain.Role;
import com.tustanovskyy.taxi.domain.request.EditUserRequest;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.domain.response.LoginResponse;
import com.tustanovskyy.taxi.exception.SmsRateLimitException;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.mapper.RideMapper;
import com.tustanovskyy.taxi.mapper.UserMapper;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.security.JwtTokenUtil;
import com.tustanovskyy.taxi.service.validatior.UserValidator;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
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
    private final SmsRateLimiter smsRateLimiter;
    private final UserMapper userMapper;
    private final RideMapper rideMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenUtil jwtTokenUtil;
    private final UserValidator userValidator;

    public User createUser(SignUpRequest user) {
        userValidator.validateSignUpRequest(user);
        var created = userRepository.save(userMapper.signUpRequestToUser(user)
                .setPassword(passwordEncoder.encode(user.getPassword())));
        sendUserPhoneVerification(created);
        return created;
    }

    public User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found"));
    }

    public User findByPhoneNumber(String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ValidationException("User not found"));
    }

    /**
     * Fetches multiple users in a single query, keyed by id. Used to avoid N+1 lookups
     * when mapping lists of entities (e.g. chat participants) to responses.
     */
    public Map<String, User> findUsersByIds(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return StreamSupport.stream(userRepository.findAllById(userIds).spliterator(), false)
                .collect(Collectors.toMap(User::getId, Function.identity()));
    }

    public LoginResponse validateCode(String code, String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        smsService.checkVerification(phoneNumber, code);

        if (!user.isRegistrationCompleted()) {
            user.setRegistrationCompleted(true);
            user.setLastTimePhoneVerified(LocalDateTime.now());
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

        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPasswordForgot(false);
        return createLoginResponse(userRepository.save(user));
    }

    public boolean forgotPassword(String phoneNumber) {
        User user = getUserByPhoneNumber(phoneNumber);
        log.info("user with number {} forgot password", phoneNumber);
        user.setPasswordForgot(true);
        sendUserPhoneVerification(user);
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

    public User addHomeAddress(Place homeAddress, String phoneNumber) {
        return userRepository.findByPhoneNumber(phoneNumber)
                .map(user -> user.setHomeAddress(rideMapper.placeDtoToPlace(homeAddress)))
                .map(userRepository::save)
                .orElseThrow(() -> new RuntimeException("failed to add home address"));
    }

    public void sendUserPhoneVerification(String phoneNumber) {
        sendUserPhoneVerification(getUserByPhoneNumber(phoneNumber));
    }

    /**
     * Sends a verification SMS to the given user, enforcing an escalating cooldown
     * (1 / 5 / 10 minutes) based on the user's recent SMS send history to prevent spam.
     */
    public void sendUserPhoneVerification(User user) {
        long secondsRemaining = smsRateLimiter.secondsUntilNextAllowedSend(user.getSmsSentAt());
        if (secondsRemaining > 0) {
            throw new SmsRateLimitException(secondsRemaining);
        }
        String status = smsService.sendVerification(user.getPhoneNumber());
        log.info("status {} of sms sending to {}", status, user.getPhoneNumber());
        user.setSmsSentAt(smsRateLimiter.registerSend(user.getSmsSentAt()));
        userRepository.save(user);
    }

    private static String getRandomNumberString() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    public User editUser(EditUserRequest editUserRequest, String phoneNumber) {
        User user = this.getUserByPhoneNumber(phoneNumber);
        user.setFirstName(editUserRequest.getFirstName());
        user.setLastName(editUserRequest.getLastName());
        user.setEmail(editUserRequest.getEmail());
        return userRepository.save(user);
    }

    public void deleteUser(String phoneNumber) {
        User user = this.getUserByPhoneNumber(phoneNumber);
        userRepository.deleteById(user.getId());
    }

    public void checkAccess(String phoneNumber, String userId) {
        var currentUser = this.getUserByPhoneNumber(phoneNumber);
        if (!currentUser.getId().equals(userId) || Role.ADMIN.equals(currentUser.getRole())) {
            throw new ValidationException("Access denied");
        }
    }
}

