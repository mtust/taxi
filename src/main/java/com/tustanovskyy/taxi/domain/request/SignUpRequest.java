package com.tustanovskyy.taxi.domain.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SignUpRequest {
    private String firstName;
    private String lastName;
    @Email
    private String email;
    private String phoneNumber;

    private String password;
    private String passwordRetry;

}
