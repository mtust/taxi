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
    private String code;
    private String password;
    private String passwordRetry;
}
