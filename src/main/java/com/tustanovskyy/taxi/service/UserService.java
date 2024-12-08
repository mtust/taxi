package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.domain.request.SignUpRequest;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.exception.VerificationCodeException;
import com.tustanovskyy.taxi.mapper.UserMapper;
import com.tustanovskyy.taxi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final SmsService smsService;
    private final UserMapper userMapper;


    public User createUser(SignUpRequest user) {
        validateUser(user);
        var created = userRepository.save(userMapper.userDtoToUser(user));
        sendVerificationCode(created);
        return created;
    }


    public User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("user not found"));
    }


    public void sendVerificationCode(String phoneNumber) {
        sendVerificationCode(userRepository
                .findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new ValidationException("user not found")));
    }

    public User validateCode(String code, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber).orElseThrow(() ->
                new VerificationCodeException("user by phone number not found"));
        if (user.getVerificationCodeDate() == null || LocalDateTime.now().minusMinutes(10)
                .isAfter(user.getVerificationCodeDate())) {
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

    private void sendVerificationCode(User user) {
        String code = getRandomNumberString();
        var phoneNumber = user.getPhoneNumber();
        LocalDateTime now = LocalDateTime.now();
        user.setVerificationCodeDate(now);
        user.setVerificationCode(code);
        userRepository.save(user);
        smsService.sendSms(phoneNumber, code);
    }

    private void validateUser(SignUpRequest user) {
        var phoneNumber = user.getPhoneNumber();
        if (StringUtils.isEmpty(phoneNumber)) {
            throw new ValidationException("phone number is empty");
        }
        if (userRepository.findByPhoneNumber(phoneNumber).isPresent()) {
            throw new ValidationException("User with the phone number already exists");
        }
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
