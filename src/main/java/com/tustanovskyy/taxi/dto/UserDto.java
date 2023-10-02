package com.tustanovskyy.taxi.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String name;
    private String surname;
    @Email
    private String email;
    private String phoneNumber;
}
