package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.jpa.Homes;
import java.util.List;
import java.util.Map;

public interface SlackService {

  void syncPreferredHomes();

  Map<String, List<Homes>> getNewHomes();
}
