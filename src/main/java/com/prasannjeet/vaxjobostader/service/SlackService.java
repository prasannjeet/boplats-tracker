package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;

import java.util.List;

public interface SlackService {
  void syncPreferredHomes(HomeSearchConfig config);
  void sendNotificationOfLastDayToApplyForHomes(List<House> homes, HomeSearchConfig config, boolean forceSend);
}
