package com.tustanovskyy.taxi.service;

import lombok.Value;

public interface SmsService {
    void sendSms(String to, String message);
}
