package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;

import java.util.Date;
import java.util.List;

public interface HomeService {

  default ResponseRoot getAllItemsFromApi() { return null; }

  default void updateDatabase(List<Result> results) {}

  default List<House> getFilteredItems(int rentPerMonth, int rentPerMonth2, double minArea,
      double maxArea,
      double queuePoints, int objectSubGroupNo, int objectSubGroupNo2, int marketPlaceNo,
      int companyNo, Date date) { return List.of(); }

  List<House> getHomesWithLastDateToday(HomeSearchConfig config);

}