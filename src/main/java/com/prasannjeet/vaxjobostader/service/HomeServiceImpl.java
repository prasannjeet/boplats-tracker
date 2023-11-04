package com.prasannjeet.vaxjobostader.service;

import static com.prasannjeet.vaxjobostader.client.dto.request.RequestRoot.getDefaultRequest;
import static com.prasannjeet.vaxjobostader.client.dto.request.RequestRoot.getDefaultStudentRequest;
import static com.prasannjeet.vaxjobostader.util.HomeResultConverter.convertResultsToHomes;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.request.RequestRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.exception.ClientException;
import com.prasannjeet.vaxjobostader.jpa.Constants;
import com.prasannjeet.vaxjobostader.jpa.ConstantsRepository;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor
@Slf4j
public class HomeServiceImpl implements HomeService {

  private final VaxjobostaderClient client;
  private final HomesRepository homesRepository;
  private final ConstantsRepository constantsRepository;

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
      log.error("Some error occurred while fetching all items from API.", e);
      throw new ClientException(e);
    }
  }

  @Override
  @Modifying(clearAutomatically = true)
  public void updateDatabase(List<Result> results) {
    List<Homes> homes = convertResultsToHomes(results);
    homesRepository.saveAll(homes);
    LocalDateTime todayTime = LocalDateTime.now();

    // Fetch the Constants entity with id 1, or create a new one if it doesn't exist
    Constants constants = constantsRepository.findById(1)
        .orElseGet(() -> new Constants(1, null)); // If not found, create a new instance with id 1

    constants.setLastUpdated(todayTime); // Set the last updated time
    constantsRepository.save(constants); // Save the Constants entity
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
