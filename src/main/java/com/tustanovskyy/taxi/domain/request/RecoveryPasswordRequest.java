package com.tustanovskyy.taxi.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RecoveryPasswordRequest {
    private String phoneNumber;
    /** Firebase ID token obtained client-side after completing phone sign-in via the Firebase Auth SDK. */
    private String idToken;
    private String password;
    private String passwordRetry;
}
