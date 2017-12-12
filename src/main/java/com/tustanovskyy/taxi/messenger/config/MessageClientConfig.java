package com.tustanovskyy.taxi.messenger.config;

import com.github.messenger4j.Messenger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MessageClientConfig {


    @Bean
    public Messenger messenger(@Value("${spring.social.facebook.page.accessToken}") String pageAccessToken,
                               @Value("${spring.social.facebook.app-secret}") final String appSecret,
                               @Value("${messenger4j.verifyToken}") final String verifyToken) {
        return Messenger.create(pageAccessToken, appSecret, verifyToken);
    }

}
