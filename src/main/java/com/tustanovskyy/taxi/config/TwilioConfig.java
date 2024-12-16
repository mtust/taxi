package com.tustanovskyy.taxi.config;

import com.twilio.Twilio;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {

    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Getter
    @Value("${twilio.serviceId}")
    private String twilioServiceId;

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

}
