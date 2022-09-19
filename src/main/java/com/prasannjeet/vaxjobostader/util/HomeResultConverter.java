package com.prasannjeet.vaxjobostader.util;

import static java.util.stream.Collectors.toList;
import static lombok.AccessLevel.PRIVATE;

import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import java.util.ArrayList;
import java.util.List;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class HomeResultConverter {

  public static Result convertHomeToResult(Homes home) {
    Result result = new Result();

    result.setObjectMainGroupDescription(null);
    result.setObjectSubGroupDescription(null);
    result.setRowId(home.getRowId());
    result.setSyndicateNo(home.getSyndicateNo());
    result.setCompanyNo(home.getCompanyNo());
    result.setAreaNo(home.getAreaNo());
    result.setHouseNo(home.getHouseNo());
    result.setEntranceNo(home.getEntranceNo());
    result.setObjectNo(home.getObjectNo());
    result.setObjectMainGroupNo(home.getObjectMainGroupNo());
    result.setObjectSubGroupNo(home.getObjectSubGroupNo());
    result.setObjectTypeDescription(home.getObjectTypeDescription());
    result.setObjectAreaSort(home.getObjectAreaSort());
    result.setObjectSubDescription(home.getObjectSubDescription());
    result.setObjectArea(home.getObjectArea());
    result.setObjectFloor(home.getObjectFloor());
    result.setSeekAreaNo(home.getSeekAreaNo());
    result.setMarketPlaceNo(home.getMarketPlaceNo());
    result.setHouseFormNo(home.getHouseFormNo());
    result.setPropertyNo(home.getPropertyNo());
    result.setRentPerMonthSort(home.getRentPerMonthSort());
    result.setRentPerMonth(home.getRentPerMonth());
    result.setFirstEstateImageUrl(home.getFirstEstateImageUrl());
    result.setBoardNo(home.getBoardNo());
    result.setDescription(home.getDescription());
    result.setPlaceName(home.getPlaceName());
    result.setSeekAreaDescription(home.getSeekAreaDescription());
    result.setStreetName(home.getStreetName());
    result.setStreetNo(home.getStreetNo());
    result.setStreetChar(home.getStreetChar());
    result.setMarketPlaceDescription(home.getMarketPlaceDescription());
    result.setCountInterest(home.getCountInterest());
    result.setFirstInfoTextShort(home.getFirstInfoTextShort());
    result.setFirstInfoText(home.getFirstInfoText());
    result.setEndPeriodMP(home.getEndPeriodMP());
    result.setEndPeriodMPDateString(home.getEndPeriodMPDateString());
    result.setQueueDate(home.getQueueDate());
    result.setQueueDateString(home.getQueueDateString());
    result.setQueuePoints(home.getQueuePoints());
    result.setQueuePositionCut(home.getQueuePositionCut());
    result.setFreeFrom(home.getFreeFrom());
    result.setDesiredFreeFrom(home.getDesiredFreeFrom());
    result.setDesiredFreeFromString(home.getDesiredFreeFromString());
    result.setSeekAreaUrl(home.getSeekAreaUrl());
    result.setLatitude(home.getLatitude());
    result.setLongitude(home.getLongitude());
    result.setArriveMarketPlace(home.getArriveMarketPlace());
    result.setArriveMarketPlaceDateString(home.getArriveMarketPlaceDateString());
    result.setPublishingDate(home.getPublishingDate());
    result.setPublishingDateString(home.getPublishingDateString());
    result.setTradingAdvertisement(home.isTradingAdvertisement());
    result.setStatusDescriptionClient(home.getStatusDescriptionClient());
    result.setUseFilter(home.getUseFilter());
    result.setSyndicateHouseFormDescriptionGrouped(home.getSyndicateHouseFormDescriptionGrouped());
    result.setSyndicatePropertyDescriptionGrouped(home.getSyndicatePropertyDescriptionGrouped());
    result.setMapUrl(home.getMapUrl());
    result.setQueueCutShow(home.getQueueCutShow());
    result.setStreet(home.getStreet());
    result.setInterestOpensDateString(home.getInterestOpensDateString());
    result.setSyndicateMarketPlaceImageAlt(home.getSyndicateMarketPlaceImageAlt());
    result.setRankUpInSearchResult(home.isRankUpInSearchResult());
    result.setSellerNote(home.getSellerNote());
    result.setProperties(home.getProperties());
    result.setHouseForms(home.getHouseForms());
    result.setGroundRent(home.getGroundRent());
    result.setStatusNo(home.getStatusNo());
    result.setStatusDescription(home.getStatusDescription());
    result.setPropertyNo(home.getPropertyNo());
    result.setEstateNo(home.getEstateNo());
    result.setPostCode(home.getPostCode());
    result.setInterestOpens(null);
    result.setObjectTags(new ArrayList<>());

    return result;
  }

  public static List<Result> convertHomesToResults(List<Homes> homes) {
    return homes.stream().map(HomeResultConverter::convertHomeToResult).collect(toList());
  }

  public static Homes convertResultToHome(Result result) {
    Homes home = new Homes();

    home.setRowId(result.getRowId());
    home.setSyndicateNo(result.getSyndicateNo());
    home.setCompanyNo(result.getCompanyNo());
    home.setAreaNo(result.getAreaNo());
    home.setHouseNo(result.getHouseNo());
    home.setEntranceNo(result.getEntranceNo());
    home.setObjectNo(result.getObjectNo());
    home.setObjectMainGroupNo(result.getObjectMainGroupNo());
    home.setObjectSubGroupNo(result.getObjectSubGroupNo());
    home.setObjectTypeDescription(result.getObjectTypeDescription());
    home.setObjectAreaSort(result.getObjectAreaSort());
    home.setObjectSubDescription(result.getObjectSubDescription());
    home.setObjectArea(result.getObjectArea());
    home.setObjectFloor(result.getObjectFloor());
    home.setSeekAreaNo(result.getSeekAreaNo());
    home.setMarketPlaceNo(result.getMarketPlaceNo());
    home.setHouseFormNo(result.getHouseFormNo());
    home.setPropertyNo(result.getPropertyNo());
    home.setRentPerMonthSort(result.getRentPerMonthSort());
    home.setRentPerMonth(result.getRentPerMonth());
    home.setFirstEstateImageUrl(result.getFirstEstateImageUrl());
    home.setBoardNo(result.getBoardNo());
    home.setDescription(result.getDescription());
    home.setPlaceName(result.getPlaceName());
    home.setSeekAreaDescription(result.getSeekAreaDescription());
    home.setStreetName(result.getStreetName());
    home.setStreetNo(result.getStreetNo());
    home.setStreetChar(result.getStreetChar());
    home.setMarketPlaceDescription(result.getMarketPlaceDescription());
    home.setCountInterest(result.getCountInterest());
    home.setFirstInfoTextShort(result.getFirstInfoTextShort());
    home.setFirstInfoText(result.getFirstInfoText());
    home.setEndPeriodMP(result.getEndPeriodMP());
    home.setEndPeriodMPDateString(result.getEndPeriodMPDateString());
    home.setQueueDate(result.getQueueDate());
    home.setQueueDateString(result.getQueueDateString());
    home.setQueuePoints(result.getQueuePoints());
    home.setQueuePositionCut(result.getQueuePositionCut());
    home.setFreeFrom(result.getFreeFrom());
    home.setDesiredFreeFrom(result.getDesiredFreeFrom());
    home.setDesiredFreeFromString(result.getDesiredFreeFromString());
    home.setSeekAreaUrl(result.getSeekAreaUrl());
    home.setLatitude(result.getLatitude());
    home.setLongitude(result.getLongitude());
    home.setArriveMarketPlace(result.getArriveMarketPlace());
    home.setArriveMarketPlaceDateString(result.getArriveMarketPlaceDateString());
    home.setPublishingDate(result.getPublishingDate());
    home.setPublishingDateString(result.getPublishingDateString());
    home.setTradingAdvertisement(result.isTradingAdvertisement());
    home.setStatusDescriptionClient(result.getStatusDescriptionClient());
    home.setUseFilter(result.getUseFilter());
    home.setSyndicateHouseFormDescriptionGrouped(result.getSyndicateHouseFormDescriptionGrouped());
    home.setSyndicatePropertyDescriptionGrouped(result.getSyndicatePropertyDescriptionGrouped());
    home.setMapUrl(result.getMapUrl());
    home.setQueueCutShow(result.getQueueCutShow());
    home.setStreet(result.getStreet());
    home.setInterestOpensDateString(result.getInterestOpensDateString());
    home.setSyndicateMarketPlaceImageAlt(result.getSyndicateMarketPlaceImageAlt());
    home.setRankUpInSearchResult(result.isRankUpInSearchResult());
    home.setSellerNote(result.getSellerNote());
    home.setProperties(result.getProperties());
    home.setHouseForms(result.getHouseForms());
    home.setGroundRent(result.getGroundRent());
    home.setStatusNo(result.getStatusNo());
    home.setStatusDescription(result.getStatusDescription());
    home.setPropertyNo(result.getPropertyNo());
    home.setEstateNo(result.getEstateNo());
    home.setPostCode(result.getPostCode());

    return home;
  }

  public static List<Homes> convertResultsToHomes(List<Result> results) {
    return results.stream().map(HomeResultConverter::convertResultToHome).collect(toList());
  }
}
