package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;

import static com.prasannjeet.vaxjobostader.service.HomeUtil.filterHomes;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class HomeServiceImpl implements HomeService {

  private final HouseRepository houseRepository;

  @Override
  public List<House> getHomesWithLastDateToday(HomeSearchConfig config) {
    // Get all houses based on user preference
    List<House> allPreferredHomes = getCurrentPreferredHomes(config);

    // Get today's date with time set to midnight for accurate comparison
    Calendar today = Calendar.getInstance();
    today.set(Calendar.HOUR_OF_DAY, 0);
    today.set(Calendar.MINUTE, 0);
    today.set(Calendar.SECOND, 0);
    today.set(Calendar.MILLISECOND, 0);

    // Get all homes whose applicationDeadline is exactly today
    return allPreferredHomes.stream().filter(house -> {
      if (house.getApplicationDeadline() == null) return false;
      Calendar endPeriodMP = Calendar.getInstance();
      endPeriodMP.setTime(house.getApplicationDeadline());
      endPeriodMP.set(Calendar.HOUR_OF_DAY, 0);
      endPeriodMP.set(Calendar.MINUTE, 0);
      endPeriodMP.set(Calendar.SECOND, 0);
      endPeriodMP.set(Calendar.MILLISECOND, 0);
      return today.equals(endPeriodMP);
    }).toList();
  }

  private List<House> getCurrentPreferredHomes(HomeSearchConfig config) {
    List<House> preferredHomes = houseRepository.findPreferredHomes(config);
    return filterHomes(preferredHomes, config);
  }

}
