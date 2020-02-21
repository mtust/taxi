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
import com.tustanovskyy.taxi.document.Ride;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.repository.RideRepository;
import com.tustanovskyy.taxi.repository.UserRepository;
import com.tustanovskyy.taxi.service.RideService;
import com.tustanovskyy.taxi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Collection;
import java.util.stream.Collectors;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Service
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
}
