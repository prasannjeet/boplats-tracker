package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.exception.ClientException;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.service.HomeService;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerErrorException;

import java.util.Date;
import java.util.List;

import static com.prasannjeet.vaxjobostader.util.HomeResultConverter.convertHomesToResults;

@RestController
@RequestMapping("/list")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
@Slf4j
public class Listing {

  private final HomeService homeService;
  private final SlackService lastUpdatedService;
  private final List<HomeSearchConfig> homeSearchConfigList;

  @GetMapping(value = "/update")
  public ResponseEntity<String> update() throws ClientException {
    int size = 0;
    try {
      log.info("Updating homes");
      ResponseRoot response = homeService.getAllItemsFromApi();
      homeService.updateDatabase(response.getResult());
      size = response.getResult().size();
    } catch (Exception e) {
      log.error("Error occurred while updating homes from API", e);
      throw new ClientException(e);
    }
    log.info("Updated homes. {} New homes added or updated.", size);
    return ResponseEntity.ok("List of homes updated. Fetched " + size + " homes.");
  }

  @GetMapping(value = "/check/{name}")
  public ResponseEntity<String> check(@PathVariable("name") String name) {
    try {
      log.info("Checking new homes via API for {}", name);
      HomeSearchConfig config = homeSearchConfigList.stream()
          .filter(c -> c.name().equalsIgnoreCase(name))
          .findFirst()
          .orElseThrow(() -> new ClientException("No config found for " + name));
      lastUpdatedService.syncPreferredHomes(config);
      return ResponseEntity.ok("Done. Please check your slack.");
    } catch (Exception e) {
      log.error("Error occurred while checking for new homes for the user {}", name, e);
      throw new ServerErrorException("Error occurred while checking for new homes", e);
    }
  }

  @GetMapping(value = "/lastday/{name}")
  public ResponseEntity<String> lastDay(@PathVariable("name") String name) {
    try {
      log.info("Checking last day to apply for homes via API for {}", name);
      HomeSearchConfig config = homeSearchConfigList.stream()
          .filter(c -> c.name().equalsIgnoreCase(name))
          .findFirst()
          .orElseThrow(() -> new ClientException("No config found for " + name));
      List<House> homes = homeService.getHomesWithLastDateToday(config);
      lastUpdatedService.sendNotificationOfLastDayToApplyForHomes(homes, config, true);
      return ResponseEntity.ok("Done. Please check your slack.");
    } catch (Exception e) {
      log.error("Error occurred while checking for last day to apply for homes for the user {}", name, e);
      throw new ServerErrorException("Error occurred while checking for last day to apply for homes", e);
    }
  }

  @GetMapping(value = "/advanced")
  public ResponseEntity<ResponseRoot> getAdvancedFilteredHomes(
      @RequestParam("minRent") int minRent,
      @RequestParam("maxRent") int maxRent,
      @RequestParam("minArea") int minArea,
      @RequestParam("maxArea") int maxArea,
      @RequestParam("points") int points,
      @RequestParam("minRooms") int minRooms,
      @RequestParam("maxRooms") int maxRooms,
      @RequestParam("excludeSenior") boolean excludeSenior,
      @RequestParam("company") int company) {

    try {
      ResponseRoot response;
      Date date = new Date();
      log.info("Querying the database now.");

      List<House> allHomes = homeService.getFilteredItems(
          minRent, maxRent, minArea, maxArea, points, minRooms, maxRooms, excludeSenior ? 9 : -1,
          company, date
      );

      log.info("Found {} results from the query.", allHomes.size());
      List<Result> results = convertHomesToResults(allHomes);

      response = new ResponseRoot();
      response.setObjectMainGroupDescription("DB");
      response.setObjectMainGroupNo(1);
      response.setResult(results);
      response.setTotalCount(results.size());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      log.error("An error occurred while processing the request.", e);
      throw new ServerErrorException("Some unknown error occurred", e);
    }
  }

}
