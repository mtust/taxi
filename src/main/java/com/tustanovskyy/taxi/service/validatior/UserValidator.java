package com.tustanovskyy.taxi.service.validatior;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.exception.ErrorCode;
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
        validate(() -> request.getPassword().equals(request.getPasswordRetry()),
                ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords do not match");
        validate(() -> StringUtils.isNotEmpty(request.getPhoneNumber()),
                ErrorCode.PHONE_NUMBER_EMPTY, "Phone number is empty");
        // A record for this phone that never finished phone verification isn't a real account
        // yet - e.g. the app was closed before entering the SMS code, or the SMS never arrived -
        // so it shouldn't permanently block signing up again. UserService#createUser reuses that
        // record (fresh name/password, new SMS) instead of inserting a duplicate. Only a
        // registration that actually completed blocks a resignup.
        validate(() -> userRepository.findByPhoneNumber(request.getPhoneNumber())
                        .map(User::isRegistrationCompleted).map(completed -> !completed).orElse(true),
                ErrorCode.USER_ALREADY_EXISTS, "User with this phone number already exists");
    }

    public void validateLogin(User user, String password, PasswordEncoder passwordEncoder) {
        validate(user::isRegistrationCompleted, ErrorCode.PHONE_NOT_VERIFIED, "Phone number not verified");
        // Intentionally the same error as "phone number not found" in UserService#login -
        // never reveal which of the two was wrong.
        validate(() -> passwordEncoder.matches(password, user.getPassword()),
                ErrorCode.INVALID_CREDENTIALS, "Invalid phone number or password");
    }

    public void validateRecoveryPasswordRequest(RecoveryPasswordRequest request) {
        validate(() -> request.getPassword().equals(request.getPasswordRetry()),
                ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords do not match");
        validate(() -> smsService.checkVerification(request.getPhoneNumber(),
                request.getCode()), ErrorCode.INVALID_VERIFICATION_CODE, "Invalid verification code");
    }
}
