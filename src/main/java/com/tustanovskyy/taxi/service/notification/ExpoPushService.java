package com.tustanovskyy.taxi.service.notification;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Thin wrapper around Expo's push API (https://exp.host/--/api/v2/push/send) - the only push
 * transport this app uses, since the client is an Expo-managed app and Expo's service fans a
 * single call out to both APNs and FCM depending on the token. A failed push must never break
 * the ride/chat operation that triggered it, so every failure is caught and logged here rather
 * than propagated.
 */
@Service
@Slf4j
public class ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestTemplate restTemplate = new RestTemplate();

    public void send(String token, String title, String body, Map<String, Object> data) {
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "application/json");

            Map<String, Object> payload = Map.of(
                    "to", token,
                    "title", title,
                    "body", body,
                    "data", data
            );

            restTemplate.postForEntity(EXPO_PUSH_URL, new HttpEntity<>(payload, headers), String.class);
        } catch (Exception e) {
            log.warn("Failed to send push notification: {}", e.getMessage());
        }
    }
}
