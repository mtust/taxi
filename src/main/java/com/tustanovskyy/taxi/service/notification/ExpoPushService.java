package com.tustanovskyy.taxi.service.notification;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequiredArgsConstructor
public class ExpoPushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

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

            ResponseEntity<String> response =
                    restTemplate.postForEntity(EXPO_PUSH_URL, new HttpEntity<>(payload, headers), String.class);
            checkForDeliveryError(response.getBody());
        } catch (Exception e) {
            log.warn("Failed to send push notification: {}", e.getMessage());
        }
    }

    // Expo's push API responds 200 even when the push wasn't actually delivered (bad/expired
    // credentials, unregistered device, etc.) - the failure is only visible in the response body,
    // so RestTemplate throwing no exception doesn't mean the push went through.
    private void checkForDeliveryError(String responseBody) throws JsonProcessingException {
        if (responseBody == null) {
            return;
        }
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        if ("error".equals(data.path("status").asText())) {
            log.warn("Expo push delivery failed: {} ({})",
                    data.path("message").asText(),
                    data.path("details").path("error").asText());
        }
    }
}
