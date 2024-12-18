package com.tustanovskyy.taxi.domain.request;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecoveryPasswordRequest {
    private String phoneNumber;
    private String code;
    private String password;
    private String passwordRetry;
}
