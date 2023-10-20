package com.tustanovskyy.taxi.service.impl;

import com.github.messenger4j.Messenger;
import com.github.messenger4j.send.MessagePayload;
import com.github.messenger4j.send.NotificationType;
import com.github.messenger4j.send.message.TextMessage;
import com.github.messenger4j.send.recipient.IdRecipient;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.dto.UserDto;
import com.tustanovskyy.taxi.exception.ValidationException;
import com.tustanovskyy.taxi.exception.VerificationCodeException;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.SmsService;
import com.tustanovskyy.taxi.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Random;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Value("${google.map.api.key}")
    private String googleMapApiKey;
    private final UserRepository userRepository;
    private final RideService rideService;
    private final SmsService smsService;

    private Messenger messenger;

    public UserServiceImpl(UserRepository userRepository, RideService rideService, SmsService smsService) {
        this.userRepository = userRepository;
        this.rideService = rideService;
        this.smsService = smsService;
    }

//    public void createRideFromFacebook(String userFacebookId, String text) throws Exception {
//        User user = userRepository.findByFacebookId(userFacebookId);
//        if (user == null) {
//            user = userRepository.save(user);
//        }
//        GeoApiContext context = new GeoApiContext.Builder()
//                .apiKey(googleMapApiKey)
//                .build();
//        GeocodingResult[] results = GeocodingApi.geocode(context,
//                text).await();
//        Gson gson = new GsonBuilder().setPrettyPrinting().create();
//        System.out.println(gson.toJson(results[0].addressComponents));
//        String resultText = gson.toJson(results[0].addressComponents);
//        final IdRecipient recipient = IdRecipient.create(userFacebookId);
//        final NotificationType notificationType = NotificationType.REGULAR;
//        final String metadata = "DEVELOPER_DEFINED_METADATA";
//
//        final TextMessage textMessage = TextMessage.create(resultText, empty(), of(metadata));
//        this.messenger.queryUserProfile(userFacebookId);
//        final MessagePayload messagePayload = MessagePayload.create(recipient, textMessage, of(notificationType));
//        this.messenger.send(messagePayload);
//    }

    @Override
    public User createUser(UserDto user) {
        validateUser(user);
        var created = userRepository.save(User.builder()
                        .email(user.getEmail())
                        .name(user.getName())
                        .surname(user.getSurname())
                        .phoneNumber(user.getPhoneNumber())
                .build());
        sendVerificationCode(created);
        return created;
    }

    @Override
    public User findUser(Long userId) {
        return userRepository.findById(userId).get();
    }

    @Override
    public void sendVerificationCode(String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new ValidationException("user not found");
        }
        sendVerificationCode(user);
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
    public void validateCode(String code, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new VerificationCodeException("user by phone number not found");
        }
        if (user.getVerificationCodeDate() == null || LocalDateTime.now().minusMinutes(10).isBefore(user.getVerificationCodeDate())) {
            throw new VerificationCodeException("please request new sms verification code");
        }
        if (!user.getVerificationCode().equals(code)) {
            throw new VerificationCodeException("validation code not correct");
        }
    }

    private void validateUser(UserDto user) {
        if (StringUtils.isEmpty(user.getPhoneNumber())) {
            throw new ValidationException("phone number is empty");
        }
        User existed = userRepository.findByPhoneNumber(user.getPhoneNumber());
        if (existed != null && existed.isRegistrationCompleted()) {
            throw new ValidationException("user with this phone number already registered");
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
