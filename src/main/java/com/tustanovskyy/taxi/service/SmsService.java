package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsService {

    private final TwilioConfig twilioConfig;

    public void sendSms(String toPhoneNumber, String code) {
        Message message = Message.creator(
                        new PhoneNumber(toPhoneNumber), // to
                        new PhoneNumber(twilioConfig.getTwilioServiceId()),
                        "Your taxi app verification code: " + code)
                .create();
        log.info("message: {} \nsid: {}", message.getBody(), message.getSid());
    }
}
