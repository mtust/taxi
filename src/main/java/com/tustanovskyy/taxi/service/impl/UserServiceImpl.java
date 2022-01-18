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
import com.tustanovskyy.taxi.exception.VerificationCodeException;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Random;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Value("${google.map.api.key}")
    private String googleMapApiKey;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RideService rideService;

    Messenger messenger;

    @Override
    public void createRideFromFacebook(String userFacebookId, String text) throws Exception {
        User user = userRepository.findByFacebookId(userFacebookId);
        if (user == null) {
            user = userRepository.save(user);
        }
        GeoApiContext context = new GeoApiContext.Builder()
                .apiKey(googleMapApiKey)
                .build();
        GeocodingResult[] results = GeocodingApi.geocode(context,
                text).await();
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println(gson.toJson(results[0].addressComponents));
        String resultText = gson.toJson(results[0].addressComponents);
        final IdRecipient recipient = IdRecipient.create(userFacebookId);
        final NotificationType notificationType = NotificationType.REGULAR;
        final String metadata = "DEVELOPER_DEFINED_METADATA";

        final TextMessage textMessage = TextMessage.create(resultText, empty(), of(metadata));
        this.messenger.queryUserProfile(userFacebookId);
        final MessagePayload messagePayload = MessagePayload.create(recipient, textMessage, of(notificationType));
        this.messenger.send(messagePayload);
    }

//    public Collection<User> findPartners(String userId) {
//        User user = userRepository.findOne(userId);
//        return findPartners(user);
//    }

//    @Override
//    @Transactional
//    public Collection<User> findPartners(User user) {
//        Ride currentRide = user.getActiveRide();
//        return rideService.findPartnersRide(currentRide.getId())
//                .stream()
//                .map(Ride::getUser)
//                .collect(Collectors.toList());
//    }

    @Override
    public User createUser(User user) {
        User loadedUser = findUserByFacebookId(user.getFacebookId());
        if (loadedUser == null) {
            return userRepository.save(user);
        } else {
            return loadedUser;
        }
    }

    @Override
    public User findUser(Long userId) {
        return userRepository.findById(userId).get();
    }

    @Override
    public User findUserByFacebookId(String facebookId) {
        return userRepository.findByFacebookId(facebookId);
    }

    @Override
    public void sendVerificationCode(String phoneNumber) {
        String code = getRandomNumberString();
        User user = userRepository.findByPhoneNumber(phoneNumber);
        Timestamp now = new Timestamp(LocalDateTime.now().toDate().getTime());
        if (user == null) {
            User newUser = new User();
            newUser.setVerificationCode(code);
            newUser.setPhoneNumber(phoneNumber);
            newUser.setVerificationCodeDate(now);
            newUser.setCreatedDate(now);
            userRepository.save(newUser);
        } else {
            user.setVerificationCodeDate(now);
            user.setVerificationCode(code);
            userRepository.save(user);
        }
        sendSms(phoneNumber, code);
    }

    @Override
    public void validateCode(String code, String phoneNumber) {
        User user = userRepository.findByPhoneNumber(phoneNumber);
        if (user == null) {
            throw new VerificationCodeException("user by phone number not found");
        }
        if (user.getVerificationCodeDate() == null || LocalDateTime.now().minusMinutes(10).toDate().getTime() > user.getVerificationCodeDate().getTime()) {
            throw new VerificationCodeException("please request new sms verification code");
        }
        if (!user.getVerificationCode().equals(code)) {
            throw new VerificationCodeException("validation code not correct");
        }
    }

    private void sendSms(String phoneNumber, String code) {
        Twilio.init("AC3a95b14e95eca5575e7561f77048dd28", "d1191a500e04e6d761dc0acb50221503");
        Message message = Message.creator(
                        new PhoneNumber(phoneNumber), // to
                        new PhoneNumber("+19285775178"),
                        "Your taxi app verification code: " + code)
                .create();
        log.info("message: " + message.getBody() + " " + message.getSid());
    }


    public static String getRandomNumberString() {
        // It will generate 6 digit random Number.
        // from 0 to 999999
        Random rnd = new Random();
        int number = rnd.nextInt(999999);

        // this will convert any number sequence into 6 character.
        return String.format("%06d", number);
    }
}
