package com.prasannjeet.vaxjobostader.testbeans;

import com.prasannjeet.vaxjobostader.service.SlackService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class Config {

    @Bean
    @Primary
    public SlackService testSlackService() {
        return new SlackServiceImpl();
    }
}
