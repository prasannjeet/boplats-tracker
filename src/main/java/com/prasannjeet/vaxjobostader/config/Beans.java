package com.prasannjeet.vaxjobostader.config;

import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomesRepository;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.SlackServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class Beans {

    private final HouseRepository houseRepository;
    private final UserSelectedHomesRepository userSelectedHomesRepository;
    private final AppConfig appConfig;

    @Bean
    public SlackService lastUpdatedService() {
        return new SlackServiceImpl(houseRepository, userSelectedHomesRepository, appConfig);
    }
}
