package com.prasannjeet.vaxjobostader.testbeans;

import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class SlackServiceImpl implements SlackService {
    @Override
    public void syncPreferredHomes(HomeSearchConfig config) {
        log.info("syncPreferredHomes called with config: {}", config);
    }

    @Override
    public void sendNotificationOfLastDayToApplyForHomes(List<House> homes, HomeSearchConfig config, boolean forceSend) {
        log.info("sendNotificationOfLastDayToApplyForHomes called with config: {}", config);
    }
}
