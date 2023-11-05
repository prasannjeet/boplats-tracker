package com.prasannjeet.vaxjobostader.testbeans;

import java.util.List;

import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SlackServiceImpl implements SlackService {
    @Override
    public void syncPreferredHomes(HomeSearchConfig config) {
        log.info("syncPreferredHomes called with config: {}", config);
    }

    @Override
    public void sendNotificationOfLastDayToApplyForHomes(List<Homes> homes, HomeSearchConfig config, boolean forceSend) {
        log.info("sendNotificationOfLastDayToApplyForHomes called with config: {}", config);
    }
}
