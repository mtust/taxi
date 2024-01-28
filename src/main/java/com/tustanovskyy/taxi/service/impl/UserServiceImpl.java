package com.tustanovskyy.taxi.service.impl;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.UserDto;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.exception.VerificationCodeException;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.SmsService;
import com.tustanovskyy.taxi.service.UserService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
@AllArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final SmsService smsService;

    @Override
    public User createUser(UserDto user) {
        validateUser(user);
        var created = userRepository.save(User.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .build());
        sendVerificationCode(created);
        return created;
    }

    @Override
    public User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("user not found"));
    }

    @Override
    public void sendVerificationCode(String phoneNumber) {
        sendVerificationCode(userRepository
                .findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ValidationException("user not found")));
    }

    private void sendVerificationCode(User user) {
        String code = getRandomNumberString();
        var phoneNumber = user.getPhoneNumber();
        LocalDateTime now = LocalDateTime.now();
        user.setVerificationCodeDate(now);
        user.setVerificationCode(code);
        userRepository.save(user);
        smsService.sendSms(phoneNumber, code);
    }

    @Override
    public User validateCode(String code, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow(() ->
                new VerificationCodeException("user by phone number not found"));
        if (user.getVerificationCodeDate() == null || LocalDateTime.now().minusMinutes(10).isAfter(user.getVerificationCodeDate())) {
            throw new VerificationCodeException("please request new sms verification code");
        }
        if (!user.getVerificationCode().equals(code)) {
            throw new VerificationCodeException("validation code not correct");
        }
        if (!user.isRegistrationCompleted()) {
            user.setRegistrationCompleted(true);
            user = userRepository.save(user);
        }
        return user;
    }

    private void validateUser(UserDto user) {
        if (StringUtils.isEmpty(user.getPhoneNumber())) {
            throw new ValidationException("phone number is empty");
        }
        userRepository.findByPhoneNumber(user.getPhoneNumber())
                .ifPresent(u -> {
                    throw new ValidationException("user with phoneNumber already exist");
                });
    }

    private static String getRandomNumberString() {
        // It will generate 6 digit random Number.
        // from 0 to 999999
        Random rnd = new Random();
        int number = rnd.nextInt(999999);

        // this will convert any number sequence into 6 character.
        return String.format("%06d", number);
    }
}
