package com.prasannjeet.vaxjobostader.util;

import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.jpa.House;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class HomeResultConverter {

  public static Result convertHomeToResult(House home) {
    Result result = new Result();

    // Map new fields to old structure so the React UI still works
    result.setObjectNo(home.getId());
    result.setRowId(home.getLocalId());
    
    // Use address for street and streetName
    result.setStreet(home.getAddress());
    result.setStreetName(home.getAddress());

    result.setDescription(home.getDescription());

    // Fix null issues before unboxing to primitive
    if (home.getRent() != null) {
      result.setRentPerMonthSort(home.getRent().intValue());
      result.setRentPerMonth(home.getRent().toString());
    } else {
      result.setRentPerMonthSort(0);
      result.setRentPerMonth("0");
    }

    if (home.getArea() != null) {
      result.setObjectAreaSort(home.getArea());
      result.setObjectArea(home.getArea().toString());
    } else {
      result.setObjectAreaSort(0.0);
      result.setObjectArea("0.0");
    }

    if (home.getRooms() != null) {
      result.setObjectSubGroupNo(home.getRooms());
      result.setObjectTypeDescription(home.getRooms() + " rum och kök");
    }

    result.setFirstEstateImageUrl(home.getImageUrl());
    result.setFreeFrom(home.getAvailableFrom() != null ? home.getAvailableFrom().toString() : null);

    result.setEndPeriodMP(home.getApplicationDeadline());
    if (home.getApplicationDeadline() != null) {
        result.setEndPeriodMPDateString(home.getApplicationDeadline().toString());
    }

    // Set some defaults to prevent UI crashes if it expects these
    result.setCountInterest(0);
    result.setQueuePoints(0.0);
    result.setMarketPlaceNo(0);
    result.setSyndicateNo(0);
    result.setCompanyNo(0);
    result.setObjectTags(new ArrayList<>());
    
    return result;
  }

  public static List<Result> convertHomesToResults(List<House> homes) {
    return homes.stream().map(HomeResultConverter::convertHomeToResult).collect(toList());
  }

}
