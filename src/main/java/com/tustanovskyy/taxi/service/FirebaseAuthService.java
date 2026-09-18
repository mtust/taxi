package com.tustanovskyy.taxi.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;
import com.tustanovskyy.taxi.exception.ErrorCode;
import com.tustanovskyy.taxi.exception.ValidationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FirebaseAuthService {

    /**
     * Verifies a Firebase ID token (issued client-side once the user completes phone sign-in
     * via the Firebase Auth SDK) and returns the phone number it was verified for. The phone
     * number comes from the token's signed claims, not from anything the client asserts
     * separately, so it can't be spoofed independently of passing Firebase's own SMS challenge.
     */
    public String verifyPhoneNumber(String idToken) {
        FirebaseToken decoded;
        try {
            decoded = FirebaseAuth.getInstance().verifyIdToken(idToken);
        } catch (FirebaseAuthException | IllegalArgumentException e) {
            log.info("Firebase ID token verification failed: {}", e.getMessage());
            throw new ValidationException(ErrorCode.INVALID_VERIFICATION_CODE, "Invalid verification code");
        }
        Object phoneNumber = decoded.getClaims().get("phone_number");
        if (!(phoneNumber instanceof String) || ((String) phoneNumber).isEmpty()) {
            throw new ValidationException(ErrorCode.INVALID_VERIFICATION_CODE, "Token has no verified phone number");
        }
        return (String) phoneNumber;
    }
}
