package com.prasannjeet.vaxjobostader.jpa;

import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HomesRepository extends JpaRepository<Homes, String> {

  // ObjectSubGroupNo is room count
  List<Homes> findAllByRentPerMonthSortBetweenAndObjectAreaSortBetweenAndQueuePointsLessThanAndObjectSubGroupNoBetweenAndMarketPlaceNoIsNotAndCompanyNoIsAndEndPeriodMPAfter(
      int rentPerMonth, int rentPerMonth2, double minArea, double maxArea,
      double queuePoints, int objectSubGroupNo, int objectSubGroupNo2, int marketPlaceNo,
      int companyNo, Date date);

}
