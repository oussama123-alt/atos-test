package com.example.atosTest.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(RegistrationProperties.class)
public class ApplicationConfig {

    /**
     * System clock, injected wherever "now" is needed so that time-dependent rules
     * (such as the age check) can be tested deterministically.
     */
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
