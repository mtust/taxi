package com.tustanovskyy.taxi.service.validatior;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.repository.UserRepository;
import java.time.LocalDateTime;
import com.tustanovskyy.taxi.service.SmsService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator extends BaseValidator{

    private final UserRepository userRepository;
    private final SmsService smsService;

    @Value("${taxi.user.code.active.minutes}")
    private Integer validationCodeActiveTime;

    public void validateSignUpRequest(SignUpRequest request) {
        validate(() -> request.getPassword().equals(request.getPasswordRetry()), "Passwords do not match");
        validate(() -> StringUtils.isNotEmpty(request.getPhoneNumber()), "Phone number is empty");
        validate(() -> userRepository.findByPhoneNumber(request.getPhoneNumber()).isEmpty(),
                "User with this phone number already exists");
    }

    public void validateLogin(User user, String password, PasswordEncoder passwordEncoder) {
        validate(user::isRegistrationCompleted, "Phone number not verified");
        validate(() -> passwordEncoder.matches(password, user.getPassword()), "Invalid password");
    }

    public void validateRecoveryPasswordRequest(RecoveryPasswordRequest request) {
        validate(() -> request.getPassword().equals(request.getPasswordRetry()), "Passwords do not match");
        validate(() -> smsService.checkVerification(request.getPhoneNumber(),
                request.getPassword()), "Invalid verification code");
    }
}
