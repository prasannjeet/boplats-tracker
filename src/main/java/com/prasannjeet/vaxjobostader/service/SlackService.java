package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;

import java.util.List;
import java.util.Map;

public interface SlackService {

  void syncPreferredHomes(HomeSearchConfig config);

  Map<String, List<Homes>> getNewHomes(HomeSearchConfig config);
}
