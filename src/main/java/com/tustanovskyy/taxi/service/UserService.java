package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.UserDto;

public interface UserService {

    User createUser(UserDto user);

    User findUser(Long userId);

    void sendVerificationCode(String phoneNumber);

    void validateCode(String code, String phoneNumber);
}
