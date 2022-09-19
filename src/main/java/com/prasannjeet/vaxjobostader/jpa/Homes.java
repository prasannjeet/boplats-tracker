package com.prasannjeet.vaxjobostader.jpa;

import com.prasannjeet.vaxjobostader.jpa.converters.StringLobConverter;
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter

@Entity
public class Homes {

  @Id
  @Column(name = "ObjectNo", nullable = false)
  private String objectNo;

  @Column(name = "RowId")
  private String rowId;

  @Column(name = "SyndicateNo")
  private int syndicateNo;

  @Column(name = "CompanyNo")
  private int companyNo;

  @Column(name = "AreaNo")
  private String areaNo;

  @Column(name = "HouseNo")
  private String houseNo;

  @Column(name = "EntranceNo")
  private String entranceNo;

  @Column(name = "ObjectMainGroupNo")
  private int objectMainGroupNo;

  @Column(name = "ObjectSubGroupNo")
  private int objectSubGroupNo;

  @Column(name = "ObjectTypeDescription")
  private String objectTypeDescription;

  @Column(name = "ObjectAreaSort")
  private double objectAreaSort;

  @Column(name = "ObjectSubDescription")
  private String objectSubDescription;

  @Column(name = "ObjectArea")
  private String objectArea;

  @Column(name = "ObjectFloor")
  private String objectFloor;

  @Column(name = "SeekAreaNo")
  private int seekAreaNo;

  @Column(name = "MarketPlaceNo")
  private int marketPlaceNo;

  @Column(name = "HouseFormNo")
  private int houseFormNo;

  @Column(name = "PropertyNo")
  private int propertyNo;

  @Column(name = "RentPerMonthSort")
  private int rentPerMonthSort;

  @Column(name = "RentPerMonth")
  private String rentPerMonth;

  @Column(name = "FirstEstateImageUrl")
  private String firstEstateImageUrl;

  @Column(name = "BoardNo")
  private String boardNo;

  @Column(name = "Description")
  private String description;

  @Column(name = "PlaceName")
  private String placeName;

  @Column(name = "SeekAreaDescription")
  private String seekAreaDescription;

  @Column(name = "StreetName")
  private String streetName;

  @Column(name = "StreetNo")
  private int streetNo;

  @Column(name = "StreetChar")
  private String streetChar;

  @Column(name = "MarketPlaceDescription")
  private String marketPlaceDescription;

  @Column(name = "CountInterest")
  private int countInterest;

  @Lob
  @Convert(converter = StringLobConverter.class)
  @Column(name = "FirstInfoTextShort")
  private String firstInfoTextShort;

  @Lob
  @Convert(converter = StringLobConverter.class)
  @Column(name = "FirstInfoText")
  private String firstInfoText;

  @Temporal(TemporalType.DATE)
  @Column(name = "EndPeriodMP")
  private Date endPeriodMP;

  @Column(name = "EndPeriodMPDateString")
  private String endPeriodMPDateString;

  @Column(name = "QueueDate")
  private Date queueDate;

  @Column(name = "QueueDateString")
  private String queueDateString;

  @Column(name = "QueuePoints")
  private double queuePoints;

  @Column(name = "QueuePositionCut")
  private int queuePositionCut;

  @Column(name = "FreeFrom")
  private String freeFrom;

  @Column(name = "DesiredFreeFrom")
  private Date desiredFreeFrom;

  @Column(name = "DesiredFreeFromString")
  private String desiredFreeFromString;

  @Column(name = "SeekAreaUrl")
  private String seekAreaUrl;

  @Column(name = "Latitude")
  private String latitude;

  @Column(name = "Longitude")
  private String longitude;

  @Column(name = "ArriveMarketPlace")
  private Date arriveMarketPlace;

  @Column(name = "ArriveMarketPlaceDateString")
  private String arriveMarketPlaceDateString;

  @Column(name = "PublishingDate")
  private Date publishingDate;

  @Column(name = "PublishingDateString")
  private String publishingDateString;

  @Column(name = "IsTradingAdvertisement")
  private boolean isTradingAdvertisement;

  @Column(name = "StatusDescriptionClient")
  private String statusDescriptionClient;

  @Column(name = "UseFilter")
  private int useFilter;

  @Column(name = "SyndicateHouseFormDescriptionGrouped")
  private String syndicateHouseFormDescriptionGrouped;

  @Column(name = "SyndicatePropertyDescriptionGrouped")
  private String syndicatePropertyDescriptionGrouped;

  @Column(name = "MapUrl")
  private String mapUrl;

  @Column(name = "QueueCutShow")
  private int queueCutShow;

  @Column(name = "Street")
  private String street;

  @Column(name = "InterestOpensDateString")
  private String interestOpensDateString;

  @Column(name = "SyndicateMarketPlaceImageAlt")
  private String syndicateMarketPlaceImageAlt;

  @Column(name = "RankUpInSearchResult")
  private boolean rankUpInSearchResult;

  @Column(name = "SellerNote")
  private String sellerNote;

  @Column(name = "Properties")
  private String properties;

  @Column(name = "HouseForms")
  private String houseForms;

  @Column(name = "GroundRent")
  private String groundRent;

  @Column(name = "StatusNo")
  private int statusNo;

  @Column(name = "StatusCode")
  private String statusCode;

  @Column(name = "StatusDescription")
  private String statusDescription;

  @Column(name = "ProjectNo")
  private String projectNo;

  @Column(name = "EstateNo")
  private String estateNo;

  @Column(name = "PostCode")
  private int postCode;

}
