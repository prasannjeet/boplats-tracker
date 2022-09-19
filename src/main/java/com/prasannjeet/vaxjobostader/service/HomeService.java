package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.Modifying;

public interface HomeService {

  ResponseRoot getAllItemsFromApi();

  @Modifying(clearAutomatically = true)
  void updateDatabase(List<Result> results);

  @SuppressWarnings("java:S107")
  List<Homes> getFilteredItems(int rentPerMonth, int rentPerMonth2, double minArea, double maxArea,
      double queuePoints, int objectSubGroupNo, int objectSubGroupNo2, int marketPlaceNo,
      int companyNo, Date date);
}
