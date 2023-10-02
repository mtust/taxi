package com.tustanovskyy.taxi.service.impl;

import com.tustanovskyy.taxi.service.SmsService;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SmsServiceImpl implements SmsService {
    @Value("${twilio.accountSid}")
    private String accountSid;

    @Value("${twilio.authToken}")
    private String authToken;

    @Value("${twilio.serviceId}")
    private String twilioServiceId;
    @Override
    public void sendSms(String to, String code) {
        Twilio.init(accountSid, authToken);
        Message message = Message.creator(
                        new PhoneNumber(to), // to
                        new PhoneNumber(twilioServiceId),
                        "Your taxi app verification code: " + code)
                .create();
        log.info("message: " + message.getBody() + " " + message.getSid());
    }
}
