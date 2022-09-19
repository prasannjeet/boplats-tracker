package com.prasannjeet.vaxjobostader.client.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Result {

  @JsonProperty("ObjectMainGroupDescription")
  private Object objectMainGroupDescription;
  @JsonProperty("ObjectSubGroupDescription")
  private Object objectSubGroupDescription;
  @JsonProperty("RowId")
  private String rowId;
  @JsonProperty("SyndicateNo")
  private int syndicateNo;
  @JsonProperty("CompanyNo")
  private int companyNo;
  @JsonProperty("AreaNo")
  private String areaNo;
  @JsonProperty("HouseNo")
  private String houseNo;
  @JsonProperty("EntranceNo")
  private String entranceNo;
  @JsonProperty("ObjectNo")
  private String objectNo;
  @JsonProperty("ObjectMainGroupNo")
  private int objectMainGroupNo;
  @JsonProperty("ObjectSubGroupNo")
  private int objectSubGroupNo;
  @JsonProperty("ObjectTypeDescription")
  private String objectTypeDescription;
  @JsonProperty("ObjectAreaSort")
  private double objectAreaSort;
  @JsonProperty("ObjectSubDescription")
  private String objectSubDescription;
  @JsonProperty("ObjectArea")
  private String objectArea;
  @JsonProperty("ObjectFloor")
  private String objectFloor;
  @JsonProperty("SeekAreaNo")
  private int seekAreaNo;
  @JsonProperty("MarketPlaceNo")
  private int marketPlaceNo;
  @JsonProperty("HouseFormNo")
  private int houseFormNo;
  @JsonProperty("PropertyNo")
  private int propertyNo;
  @JsonProperty("RentPerMonthSort")
  private int rentPerMonthSort;
  @JsonProperty("RentPerMonth")
  private String rentPerMonth;
  @JsonProperty("FirstEstateImageUrl")
  private String firstEstateImageUrl;
  @JsonProperty("BoardNo")
  private String boardNo;
  @JsonProperty("Description")
  private String description;
  @JsonProperty("PlaceName")
  private String placeName;
  @JsonProperty("SeekAreaDescription")
  private String seekAreaDescription;
  @JsonProperty("StreetName")
  private String streetName;
  @JsonProperty("StreetNo")
  private int streetNo;
  @JsonProperty("StreetChar")
  private String streetChar;
  @JsonProperty("MarketPlaceDescription")
  private String marketPlaceDescription;
  @JsonProperty("CountInterest")
  private int countInterest;
  @JsonProperty("FirstInfoTextShort")
  private String firstInfoTextShort;
  @JsonProperty("FirstInfoText")
  private String firstInfoText;
  @JsonProperty("EndPeriodMP")
  private Date endPeriodMP;
  @JsonProperty("EndPeriodMPDateString")
  private String endPeriodMPDateString;
  @JsonProperty("QueueDate")
  private Date queueDate;
  @JsonProperty("QueueDateString")
  private String queueDateString;
  @JsonProperty("QueuePoints")
  private double queuePoints;
  @JsonProperty("QueuePositionCut")
  private int queuePositionCut;
  @JsonProperty("FreeFrom")
  private String freeFrom;
  @JsonProperty("DesiredFreeFrom")
  private Date desiredFreeFrom;
  @JsonProperty("DesiredFreeFromString")
  private String desiredFreeFromString;
  @JsonProperty("SeekAreaUrl")
  private String seekAreaUrl;
  @JsonProperty("Latitude")
  private String latitude;
  @JsonProperty("Longitude")
  private String longitude;
  @JsonProperty("ArriveMarketPlace")
  private Date arriveMarketPlace;
  @JsonProperty("ArriveMarketPlaceDateString")
  private String arriveMarketPlaceDateString;
  @JsonProperty("PublishingDate")
  private Date publishingDate;
  @JsonProperty("PublishingDateString")
  private String publishingDateString;
  @JsonProperty("IsTradingAdvertisement")
  private boolean isTradingAdvertisement;
  @JsonProperty("StatusDescriptionClient")
  private String statusDescriptionClient;
  @JsonProperty("UseFilter")
  private int useFilter;
  @JsonProperty("SyndicateHouseFormDescriptionGrouped")
  private String syndicateHouseFormDescriptionGrouped;
  @JsonProperty("SyndicatePropertyDescriptionGrouped")
  private String syndicatePropertyDescriptionGrouped;
  @JsonProperty("MapUrl")
  private String mapUrl;
  @JsonProperty("QueueCutShow")
  private int queueCutShow;
  @JsonProperty("Street")
  private String street;
  @JsonProperty("InterestOpens")
  private Object interestOpens;
  @JsonProperty("InterestOpensDateString")
  private String interestOpensDateString;
  @JsonProperty("SyndicateMarketPlaceImageAlt")
  private String syndicateMarketPlaceImageAlt;
  @JsonProperty("RankUpInSearchResult")
  private boolean rankUpInSearchResult;
  @JsonProperty("SellerNote")
  private String sellerNote;
  @JsonProperty("Properties")
  private String properties;
  @JsonProperty("HouseForms")
  private String houseForms;
  @JsonProperty("ObjectTags")
  private List<Object> objectTags;
  @JsonProperty("GroundRent")
  private String groundRent;
  @JsonProperty("StatusNo")
  private int statusNo;
  @JsonProperty("StatusCode")
  private String statusCode;
  @JsonProperty("StatusDescription")
  private String statusDescription;
  @JsonProperty("ProjectNo")
  private String projectNo;
  @JsonProperty("EstateNo")
  private String estateNo;
  @JsonProperty("PostCode")
  private int postCode;

}
