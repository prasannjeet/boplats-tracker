package com.prasannjeet.vaxjobostader.controller;

import static com.prasannjeet.vaxjobostader.util.HomeResultConverter.convertHomesToResults;

import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.Result;
import com.prasannjeet.vaxjobostader.exception.ClientException;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.service.HomeService;
import com.prasannjeet.vaxjobostader.service.SlackService;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerErrorException;

@RestController
@RequestMapping("/list")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class Listing {

  private static final Logger LOG = LoggerFactory.getLogger(Listing.class);

  private final HomeService homeService;
  private final SlackService slackService;


  @GetMapping(value = "/update")
  public ResponseEntity<String> update() throws ClientException {
    int size = 0;
    try {
      LOG.info("Updating homes");
      ResponseRoot response = homeService.getAllItemsFromApi();
      homeService.updateDatabase(response.getResult());
      size = response.getResult().size();
    } catch (Exception e) {
      LOG.error("Error occurred while updating homes from API", e);
      throw new ClientException(e);
    }
    LOG.info("Updated homes. {} New homes added or updated.", size);
    return ResponseEntity.ok("List of homes updated. Fetched " + size + " homes.");
  }

  @GetMapping(value = "/check")
  public ResponseEntity<String> check() {
    try {
      LOG.info("Checking new homes via API");
      slackService.syncPreferredHomes();
      return ResponseEntity.ok("Done. Please check your slack.");
    } catch (Exception e) {
      throw new ServerErrorException("Error occurred while checking for new homes", e);
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
      LOG.info("Querying the database now.");

      List<Homes> allHomes = homeService.getFilteredItems(
          minRent, maxRent, minArea, maxArea, points, minRooms, maxRooms, excludeSenior ? 9 : -1,
          company, date
      );

      LOG.info("Found {} results from the query.", allHomes.size());
      List<Result> results = convertHomesToResults(allHomes);

      response = new ResponseRoot();
      response.setObjectMainGroupDescription("DB");
      response.setObjectMainGroupNo(1);
      response.setResult(results);
      response.setTotalCount(results.size());

      return ResponseEntity.ok(response);
    } catch (Exception e) {
      LOG.error("An error occurred while processing the request.", e);
      throw new ServerErrorException("Some unknown error occurred", e);
    }
  }

}
