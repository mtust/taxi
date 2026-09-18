package com.tustanovskyy.taxi.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CodeRequest {
    /** Firebase ID token obtained client-side after completing phone sign-in via the Firebase Auth SDK. */
    private String idToken;
}
