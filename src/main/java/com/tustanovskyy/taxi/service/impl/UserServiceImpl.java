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
import com.google.maps.errors.ApiException;
import com.google.maps.model.GeocodingResult;
import com.tustanovskyy.taxi.document.User;
import com.tustanovskyy.taxi.repository.UserRepositoryRestResources;
import com.tustanovskyy.taxi.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

import static java.util.Optional.empty;
import static java.util.Optional.of;

@Service
public class UserServiceImpl implements UserService {

    @Value("${google.map.api.key}")
    private String googleMapApiKey;

    @Autowired
    UserRepositoryRestResources userRepositoryRestResources;

    Messenger messenger;

    @Override
    public void createRideFromFacebook(String userFacebookId, String text) throws Exception {
            User user = userRepositoryRestResources.findByFacebookId(userFacebookId);
            if (user == null) {
                user = userRepositoryRestResources.save(user);
            } else {

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
}
