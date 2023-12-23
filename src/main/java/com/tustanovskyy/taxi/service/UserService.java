package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.UserDto;

public interface UserService {

    User createUser(UserDto user);

    User findUser(String userId);

    void sendVerificationCode(String phoneNumber);

    User validateCode(String code, String phoneNumber);
}
