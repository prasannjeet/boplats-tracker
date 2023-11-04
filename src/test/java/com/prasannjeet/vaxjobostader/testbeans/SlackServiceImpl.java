package com.prasannjeet.vaxjobostader.testbeans;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public Map<String, List<Homes>> getNewHomes(HomeSearchConfig config) {
        log.info("getNewHomes called with config: {}", config);
        return new HashMap<>();
    }
}
