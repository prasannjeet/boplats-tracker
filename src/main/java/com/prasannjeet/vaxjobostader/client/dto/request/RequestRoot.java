package com.prasannjeet.vaxjobostader.client.dto.request;

import static com.prasannjeet.vaxjobostader.util.LoggingUtils.logException;
import static com.prasannjeet.vaxjobostader.util.StaticUtils.getMapper;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.prasannjeet.vaxjobostader.exception.ClientException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Slf4j
public final class RequestRoot {

  private static final String DEFAULT_ORDER = "PublishingDate desc,SeekAreaDescription desc, StreetName desc";
  @JsonProperty("CompanyNo")
  private int companyNo;
  @JsonProperty("SyndicateNo")
  private int syndicateNo;
  @JsonProperty("ObjectMainGroupNo")
  private int objectMainGroupNo;
  @JsonProperty("Advertisements")
  private List<Advertisement> advertisements;
  @JsonProperty("RentLimit")
  private RentLimit rentLimit;
  @JsonProperty("AreaLimit")
  private AreaLimit areaLimit;
  @JsonProperty("ApplySearchFilter")
  private boolean applySearchFilter;
  @JsonProperty("Page")
  private int page;
  @JsonProperty("Take")
  private int take;
  @JsonProperty("SortOrder")
  private String sortOrder;
  @JsonProperty("ReturnParameters")
  private List<String> returnParameters;

  public static List<String> getDefaultReturnParams() {
    return asList(
        "ObjectNo",
        "FirstEstateImageUrl",
        "Street",
        "SeekAreaDescription",
        "PlaceName",
        "ObjectSubDescription",
        "ObjectArea",
        "RentPerMonth",
        "MarketPlaceDescription",
        "CountInterest",
        "FirstInfoTextShort",
        "FirstInfoText",
        "EndPeriodMP",
        "FreeFrom",
        "SeekAreaUrl",
        "Latitude",
        "Longitude",
        "BoardNo"
    );
  }

  public static RequestRoot getDefaultRequest() {
    return RequestRoot.builder()
        .companyNo(0)
        .syndicateNo(1)
        .objectMainGroupNo(1)
        .advertisements(singletonList(new Advertisement(-1)))
        .rentLimit(new RentLimit(0, 20000))
        .areaLimit(new AreaLimit(0, 155))
        .applySearchFilter(true)
        .page(1)
        .take(10)
        .sortOrder(DEFAULT_ORDER)
        .returnParameters(getDefaultReturnParams())
        .build();
  }

  public static RequestRoot getDefaultStudentRequest() {
    return RequestRoot.builder()
        .companyNo(0)
        .syndicateNo(1)
        .objectMainGroupNo(2)
        .advertisements(singletonList(new Advertisement(-1)))
        .rentLimit(new RentLimit(0, 15000))
        .areaLimit(new AreaLimit(0, 150))
        .applySearchFilter(true)
        .page(1)
        .take(10)
        .sortOrder(DEFAULT_ORDER)
        .returnParameters(getDefaultReturnParams())
        .build();
  }

  @Override
  public String toString() {
    try {
      return getMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      logException(e, "Error while serializing RequestRoot Object to String", log);
      throw new ClientException(e);
    }
  }


}
