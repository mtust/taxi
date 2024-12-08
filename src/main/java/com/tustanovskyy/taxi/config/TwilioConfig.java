package com.tustanovskyy.taxi.config;

import com.twilio.Twilio;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TwilioConfig {
    private static final Logger log = LoggerFactory.getLogger(TwilioConfig.class);
    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Getter
    @Value("${twilio.serviceId}")
    private String twilioServiceId;

    public TwilioConfig() {
        log.info("sid: {} ", accountSid);
        log.info("token {} ", authToken);
        log.info("id: {} ", twilioServiceId);
        Twilio.init(accountSid, authToken);
    }

}
