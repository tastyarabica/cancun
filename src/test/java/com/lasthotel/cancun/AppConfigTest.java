package com.lasthotel.cancun;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

@Profile("test")
@ComponentScan("com.lasthotel.cancun")
@Configuration
public class AppConfigTest {
    public static LocalDate LOCAL_DATE = LocalDate.of(2021, 7, 6);

    @Bean
    public Clock clock() {
        return Clock.fixed(LOCAL_DATE.atStartOfDay(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault());
    }
}
