package com.tustanovskyy.taxi.service.notification.telegram;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class TelegramClientTest {

    private static final String BASE = "https://api.telegram.org/botTEST_TOKEN/";

    private MockRestServiceServer server;
    private TelegramClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new TelegramClient(restTemplate, new ObjectMapper(), "TEST_TOKEN");
    }

    @Test
    void sendMessagePostsTextAndUrlButtonAndReturnsTheMessageId() {
        server.expect(requestTo(BASE + "sendMessage"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(jsonPath("$.chat_id").value("@hurtom_rides"))
                .andExpect(jsonPath("$.text").value("hello"))
                .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][0].text").value("Приєднатися"))
                .andExpect(jsonPath("$.reply_markup.inline_keyboard[0][0].url").value("https://example.com/app"))
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":555}}", MediaType.APPLICATION_JSON));

        assertEquals(555L, client.sendMessage("@hurtom_rides", "hello", "Приєднатися", "https://example.com/app"));
        server.verify();
    }

    @Test
    void sendMessageWithoutJoinUrlHasNoButton() {
        server.expect(requestTo(BASE + "sendMessage"))
                .andExpect(jsonPath("$.reply_markup").doesNotExist())
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":1}}", MediaType.APPLICATION_JSON));

        assertEquals(1L, client.sendMessage("@hurtom_rides", "hello", "Приєднатися", ""));
        server.verify();
    }

    @Test
    void editMessageTextSendsTheMessageIdAndNoButton() {
        server.expect(requestTo(BASE + "editMessageText"))
                .andExpect(jsonPath("$.message_id").value(555))
                .andExpect(jsonPath("$.text").value("⛔ Вже неактуально"))
                .andExpect(jsonPath("$.reply_markup").doesNotExist())
                .andRespond(withSuccess("{\"ok\":true,\"result\":{\"message_id\":555}}", MediaType.APPLICATION_JSON));

        assertTrue(client.editMessageText("@hurtom_rides", 555L, "⛔ Вже неактуально"));
        server.verify();
    }

    @Test
    void telegramErrorsAreSwallowed() {
        server.expect(requestTo(BASE + "sendMessage"))
                .andRespond(withBadRequest().body("{\"ok\":false,\"description\":\"Bad Request: chat not found\"}")
                        .contentType(MediaType.APPLICATION_JSON));
        server.expect(requestTo(BASE + "editMessageText"))
                .andRespond(withSuccess("{\"ok\":false,\"description\":\"message is not modified\"}", MediaType.APPLICATION_JSON));

        assertNull(client.sendMessage("@nope", "hello", "Приєднатися", "https://example.com/app"));
        assertFalse(client.editMessageText("@hurtom_rides", 1L, "x"));
        server.verify();
    }

    @Test
    void networkErrorsAreSwallowed() {
        server.expect(requestTo(BASE + "sendMessage"))
                .andRespond(request -> { throw new IOException("connection reset"); });

        assertNull(client.sendMessage("@hurtom_rides", "hello", "Приєднатися", "https://example.com/app"));
    }
}
