package com.prasannjeet.vaxjobostader.config;

import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomesRepository;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.SlackServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

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

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler s = new ThreadPoolTaskScheduler();
        s.setPoolSize(1);
        s.setThreadNamePrefix("house-detail-");
        s.setWaitForTasksToCompleteOnShutdown(true);
        s.setAwaitTerminationSeconds(30);
        return s;
    }
}
