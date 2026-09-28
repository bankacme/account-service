package com.bank.account.infrastructure.config;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Random;

import com.bank.account.domain.service.AccountNumberGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock(@Value("${bank.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }

    @Bean
    public Random random() {
        return new Random();
    }

    @Bean
    public AccountNumberGenerator accountNumberGenerator() {
        return new AccountNumberGenerator();
    }
}
