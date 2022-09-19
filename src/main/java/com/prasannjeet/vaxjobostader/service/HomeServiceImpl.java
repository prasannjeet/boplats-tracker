package com.prasannjeet.vaxjobostader.service;

import static com.prasannjeet.vaxjobostader.client.dto.request.RequestRoot.getDefaultRequest;
import static com.prasannjeet.vaxjobostader.client.dto.request.RequestRoot.getDefaultStudentRequest;
import static com.prasannjeet.vaxjobostader.util.HomeResultConverter.convertResultsToHomes;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.request.RequestRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.exception.ClientException;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.LastUpdated;
import com.prasannjeet.vaxjobostader.jpa.LastUpdatedRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.repository.Modifying;

@RequiredArgsConstructor
public class HomeServiceImpl implements HomeService {

  private static final Logger LOG = LoggerFactory.getLogger(HomeServiceImpl.class);

  private final VaxjobostaderClient client;
  private final HomesRepository homesRepository;
  private final LastUpdatedRepository lastUpdatedRepository;

  @Override
  public ResponseRoot getAllItemsFromApi() {
    RequestRoot normalApartReqBody = getDefaultRequest();
    ResponseRoot rooms;

    RequestRoot studentApartReqBody = getDefaultStudentRequest();
    ResponseRoot studentRooms;
    try {
      rooms = client.getResponseHttpClient(normalApartReqBody);
      studentRooms = client.getResponseHttpClient(studentApartReqBody);

      ResponseRoot response = new ResponseRoot();
      response.setResult(
          concat(rooms.getResult().stream(), studentRooms.getResult().stream()).collect(toList()));
      response.setTotalCount(response.getResult().size());
      response.setObjectMainGroupDescription(rooms.getObjectMainGroupDescription());
      response.setObjectMainGroupNo(rooms.getObjectMainGroupNo());

      return response;
    } catch (IOException e) {
      LOG.error("Some error occurred while fetching all items from API.", e);
      throw new ClientException(e);
    }
  }

  @Override
  @Modifying(clearAutomatically = true)
  public void updateDatabase(List<Result> results) {
    List<Homes> homes = convertResultsToHomes(results);
    homesRepository.saveAll(homes);
    LocalDateTime todayTime = LocalDateTime.now();
    LastUpdated referenceById = lastUpdatedRepository.getReferenceById(1);
    referenceById.setLastUpdated(todayTime);
    lastUpdatedRepository.save(referenceById);
  }

  @Override
  public List<Homes> getFilteredItems(int rentPerMonth, int rentPerMonth2, double minArea,
      double maxArea,
      double queuePoints, int objectSubGroupNo, int objectSubGroupNo2, int marketPlaceNo,
      int companyNo, Date date) {
    return homesRepository.findAllByRentPerMonthSortBetweenAndObjectAreaSortBetweenAndQueuePointsLessThanAndObjectSubGroupNoBetweenAndMarketPlaceNoIsNotAndCompanyNoIsAndEndPeriodMPAfter(
        rentPerMonth, rentPerMonth2, minArea, maxArea, queuePoints, objectSubGroupNo,
        objectSubGroupNo2, marketPlaceNo, companyNo, date);
  }

}
