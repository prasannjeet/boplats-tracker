package com.prasannjeet.vaxjobostader.jpa;

import com.prasannjeet.vaxjobostader.legacy.HomeSearchConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

public interface HomesRepository extends JpaRepository<Homes, String> {

  // ObjectSubGroupNo is room count
  List<Homes> findAllByRentPerMonthSortBetweenAndObjectAreaSortBetweenAndQueuePointsLessThanAndObjectSubGroupNoBetweenAndEndPeriodMPAfter(
      int rentPerMonth, int rentPerMonth2, double minArea, double maxArea,
      double queuePoints, int objectSubGroupNo, int objectSubGroupNo2,
      Date date);

  default List<Homes> findPreferredHomes(HomeSearchConfig config) {
    Date date = new Date();
    int queuePoints = calculateQueuePoints(config);

    return this.findAllByRentPerMonthSortBetweenAndObjectAreaSortBetweenAndQueuePointsLessThanAndObjectSubGroupNoBetweenAndEndPeriodMPAfter(
        config.minRent(), config.maxRent(), config.minArea(), config.maxArea(), queuePoints, config.minRooms(), config.maxRooms(),
        date);
  }

  private int calculateQueuePoints(HomeSearchConfig config) {
    LocalDate today = LocalDate.now();
    LocalDate qPointsDate = getPointsDate(config.queuePointsDate());
    return config.queuePoints() + (int) (today.toEpochDay() - qPointsDate.toEpochDay());
  }

  private LocalDate getPointsDate(int queuePointsDate) {
    int year = queuePointsDate / 10000;
    int month = (queuePointsDate % 10000) / 100;
    int day = queuePointsDate % 100;

    return LocalDate.of(year, month, day);
  }

}
