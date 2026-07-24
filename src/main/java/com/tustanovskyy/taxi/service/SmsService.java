package com.tustanovskyy.taxi.service;

import com.tustanovskyy.taxi.config.TwilioConfig;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
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
                        twilioConfig.getTwilioServiceId(), // messaging service SID (MG...)
                        "Your taxi app verification code: " + code)
                .create();
        log.info("message: {} \nsid: {}", message.getBody(), message.getSid());
    }

    public String sendVerification(String toPhoneNumber) {
        Verification verification = Verification.creator(
                        twilioConfig.getVerifyServiceSid(),
                        toPhoneNumber,
                        "sms")
                .create();
        log.info("Verification sent. status={} sid={}", verification.getStatus(), verification.getSid());
        return verification.getStatus(); // "pending"
    }

    public boolean checkVerification(String toPhoneNumber, String userSubmittedCode) {
        VerificationCheck check = VerificationCheck.creator(twilioConfig.getVerifyServiceSid())
                .setTo(toPhoneNumber)
                .setCode(userSubmittedCode)
                .create();
        log.info("Verification check: status={} valid={} sid={} to={}, code={}, number={}",
                check.getStatus(), check.getValid(), check.getSid(), check.getTo(), userSubmittedCode, toPhoneNumber);
        return "approved".equals(check.getStatus());
    }
}
