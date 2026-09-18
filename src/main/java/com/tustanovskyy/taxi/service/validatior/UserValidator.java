package com.tustanovskyy.taxi.service.validatior;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.RecoveryPasswordRequest;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.repository.UserRepository;
import java.time.LocalDateTime;
import com.tustanovskyy.taxi.service.FirebaseAuthService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserValidator extends BaseValidator{

    private final UserRepository userRepository;
    private final FirebaseAuthService firebaseAuthService;

    @Value("${taxi.user.code.active.minutes}")
    private Integer validationCodeActiveTime;

    public void validateSignUpRequest(SignUpRequest request) {
        validate(() -> request.getPassword().equals(request.getPasswordRetry()),
                ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords do not match");
        validate(() -> StringUtils.isNotEmpty(request.getPhoneNumber()),
                ErrorCode.PHONE_NUMBER_EMPTY, "Phone number is empty");
        // Deliberately blocks a resignup even for a record that never finished phone
        // verification - that's now handled by directing the user to log in instead, where
        // UserService#login resends the verification SMS automatically for an incomplete
        // registration (see PHONE_NOT_VERIFIED there), rather than silently overwriting the
        // existing record's name/password here.
        validate(() -> userRepository.findByPhoneNumber(request.getPhoneNumber()).isEmpty(),
                ErrorCode.USER_ALREADY_EXISTS, "User with this phone number already exists");
    }

    public void validateLogin(User user, String password, PasswordEncoder passwordEncoder) {
        // Intentionally the same error as "phone number not found" in UserService#login -
        // never reveal which of the two was wrong. The registration-completed check lives in
        // UserService#login itself, after this - it has a side effect (resending the
        // verification SMS) that must only run once the password's already confirmed correct.
        validate(() -> passwordEncoder.matches(password, user.getPassword()),
                ErrorCode.INVALID_CREDENTIALS, "Invalid phone number or password");
    }

    public void validateRecoveryPasswordRequest(RecoveryPasswordRequest request) {
        validate(() -> request.getPassword().equals(request.getPasswordRetry()),
                ErrorCode.PASSWORDS_DO_NOT_MATCH, "Passwords do not match");
        // The token proves ownership of a phone number on its own (Firebase signs it after the
        // user completes phone sign-in) - it must match the phone number the request claims to
        // be recovering, otherwise a token for one number could be used to reset another's password.
        String verifiedPhoneNumber = firebaseAuthService.verifyPhoneNumber(request.getIdToken());
        validate(() -> verifiedPhoneNumber.equals(request.getPhoneNumber()),
                ErrorCode.INVALID_VERIFICATION_CODE, "Invalid verification code");
    }
}
