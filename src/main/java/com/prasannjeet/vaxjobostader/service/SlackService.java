package com.prasannjeet.vaxjobostader.service;

import java.util.List;

import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.SneakyThrows;

public interface SlackService {

  void syncPreferredHomes(HomeSearchConfig config);

  @SneakyThrows
  void sendNotificationOfLastDayToApplyForHomes(List<Homes> homes, HomeSearchConfig config, boolean forceSend);
}
