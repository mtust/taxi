package com.tustanovskyy.taxi.service.otp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Sends a plain SMS through SMS-fly's JSON API v2 (https://sms-fly.ua/public/api.v2.03.pdf).
 * The sender must be an alpha name registered and approved in the SMS-fly account.
 */
@Component
@Slf4j
public class SmsFlyClient {

    private final RestClient restClient = OtpHttp.restClient();
    private final ObjectMapper objectMapper;
    private final String url;
    private final String apiKey;
    private final String sender;

    public SmsFlyClient(ObjectMapper objectMapper,
                        @Value("${smsfly.url}") String url,
                        @Value("${smsfly.api-key:}") String apiKey,
                        @Value("${smsfly.sender:}") String sender) {
        this.objectMapper = objectMapper;
        this.url = url;
        this.apiKey = apiKey;
        this.sender = sender;
    }

    /**
     * @return true if SMS-fly accepted the message for delivery (not a delivery confirmation).
     */
    public boolean send(String phoneNumber, String text, Duration ttl) {
        if (apiKey == null || apiKey.isBlank() || sender == null || sender.isBlank()) {
            log.error("SMS-fly is not configured (SMSFLY_API_KEY set: {}, SMSFLY_SENDER set: {}) - cannot send SMS",
                    apiKey != null && !apiKey.isBlank(), sender != null && !sender.isBlank());
            return false;
        }
        try {
            Map<String, Object> request = Map.of(
                    "auth", Map.of("key", apiKey),
                    "action", "SENDMESSAGE",
                    "data", Map.of(
                            // International format without the leading "+".
                            "recipient", phoneNumber.replace("+", ""),
                            "channels", List.of("sms"),
                            "sms", Map.of(
                                    "source", sender,
                                    // Minutes the operator keeps retrying delivery.
                                    "ttl", Math.max(ttl.toMinutes(), 1),
                                    "text", text)));
            String body = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
            JsonNode response = objectMapper.readTree(body == null ? "{}" : body);
            if (response.path("success").asInt(0) == 1) {
                return true;
            }
            log.warn("SMS-fly rejected message: {} {}",
                    response.path("error").path("code").asText(),
                    response.path("error").path("description").asText());
            return false;
        } catch (Exception e) {
            log.warn("SMS-fly request failed: {}", e.getMessage());
            return false;
        }
    }
}
