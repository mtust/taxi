package com.tustanovskyy.taxi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot 4's Jackson autoconfiguration only registers a Jackson 3 (tools.jackson) ObjectMapper
 * bean, but ChatService and ExpoPushService still depend on the Jackson 2 (com.fasterxml.jackson)
 * ObjectMapper, so it's provided here explicitly, matching the spring.jackson.serialization config
 * in application.yml.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}
