package com.tustanovskyy.taxi.service.notification.telegram;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

/**
 * Minimal Telegram Bot API client (https://core.telegram.org/bots/api) - just the two calls the
 * rides channel needs. Like ExpoPushService, a failed call must never break the ride operation
 * that triggered it, so every failure is caught and logged here and reported as null/false.
 */
@Slf4j
public class TelegramClient {

    private static final String API_URL = "https://api.telegram.org/bot";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String botToken;

    public TelegramClient(RestTemplate restTemplate, ObjectMapper objectMapper, String botToken) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.botToken = botToken;
    }

    /** Posts a plain-text message with one URL button; returns the new message's id, or null on failure. */
    public Long sendMessage(String chatId, String text, String buttonText, String buttonUrl) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", chatId);
        payload.put("text", text);
        payload.put("disable_web_page_preview", true);
        if (buttonText != null && buttonUrl != null && !buttonUrl.isBlank()) {
            payload.put("reply_markup", Map.of("inline_keyboard",
                    List.of(List.of(Map.of("text", buttonText, "url", buttonUrl)))));
        }
        JsonNode result = call("sendMessage", payload);
        return result != null && result.hasNonNull("message_id") ? result.get("message_id").asLong() : null;
    }

    /** Replaces a message's text; omitting reply_markup also removes its button. */
    public boolean editMessageText(String chatId, long messageId, String text) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("chat_id", chatId);
        payload.put("message_id", messageId);
        payload.put("text", text);
        payload.put("disable_web_page_preview", true);
        return call("editMessageText", payload) != null;
    }

    private JsonNode call(String method, Map<String, Object> payload) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String body = restTemplate.postForObject(API_URL + botToken + "/" + method,
                    new HttpEntity<>(payload, headers), String.class);
            JsonNode response = objectMapper.readTree(body);
            if (!response.path("ok").asBoolean(false)) {
                log.warn("Telegram {} failed: {}", method, response.path("description").asText());
                return null;
            }
            return response.path("result");
        } catch (HttpStatusCodeException e) {
            // Telegram answers errors (bad chat id, bot not admin, ...) with a 4xx and a JSON reason.
            log.warn("Telegram {} failed: {} {}", method, e.getStatusCode(), e.getResponseBodyAsString());
            return null;
        } catch (Exception e) {
            // Not e.getMessage(): I/O errors include the request URL, which contains the bot token.
            log.warn("Telegram {} failed: {}", method, e.getClass().getSimpleName());
            return null;
        }
    }
}
