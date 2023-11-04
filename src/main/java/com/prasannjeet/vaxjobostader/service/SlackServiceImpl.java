package com.prasannjeet.vaxjobostader.service;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.SetUtils.difference;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.prasannjeet.vaxjobostader.client.SlackClient;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.LastUpdated;
import com.prasannjeet.vaxjobostader.jpa.LastUpdatedRepository;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.SetUtils.SetView;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Slf4j
public class SlackServiceImpl implements SlackService {

  private final HomesRepository homesRepository;
  private final LastUpdatedRepository lastUpdatedRepository;
  private final AppConfig appConfig;

  private static LocalDate getPointsDate(int queuePointsDate) {
    int year = queuePointsDate / 10000;
    int month = (queuePointsDate % 10000) / 100;
    int day = queuePointsDate % 100;

    return LocalDate.of(year, month, day);
  }

  @SneakyThrows
  @Override
  public void syncPreferredHomes(HomeSearchConfig config) {
    log.info("Checking for new Homes for Slack Notification at: {}", now());
    Map<String, List<Homes>> items = getNewHomes(config);

    List<Homes> newHomes = items.get("new");
    List<Homes> deletedHomes = items.get("deleted");
    StringBuilder sb = new StringBuilder();
    sb.append("New Homes: ").append(newHomes.size()).append("\n");
    newHomes.forEach(n -> sb.append(getHomeMessage(n)).append("\n"));
    sb.append("Deleted Homes: ").append(deletedHomes.size()).append("\n");
    deletedHomes.forEach(d -> sb.append(getHomeMessage(d)).append("\n"));

    SlackClient slackClient = new SlackClient(config.webHook());
    slackClient.sendSlackMessage(sb.toString());
    log.info("Slack message sent. Message: {}", sb);
  }

  @Override
  public Map<String, List<Homes>> getNewHomes(HomeSearchConfig config) {
    List<Homes> preferredHomes = getNewPreferredHomes(config);
    Set<String> preferredHomesObjectNo = preferredHomes.stream().map(Homes::getObjectNo)
        .collect(toSet());

    // Retrieve the last objects only once
    Set<String> lastObjects = getLastObjects();

    SetView<String> newObjects = difference(preferredHomesObjectNo, lastObjects);
    Set<String> deletedObjects = getDeletedObjects(preferredHomes);

    // Update the last updated record or create it if it doesn't exist
    LastUpdated referenceById = lastUpdatedRepository.findById(1).orElseGet(() -> {
      LastUpdated newLastUpdated = new LastUpdated();
      newLastUpdated.setId(1);
      // Initialize other properties as needed
      return newLastUpdated;
    });
    referenceById.setPreferredObjects(new ArrayList<>(preferredHomesObjectNo));
    lastUpdatedRepository.save(referenceById);

    List<Homes> newHomes = preferredHomes.stream()
        .filter(homes -> newObjects.contains(homes.getObjectNo())).toList();
    List<Homes> deletedHomes = preferredHomes.stream()
        .filter(homes -> deletedObjects.contains(homes.getObjectNo())).toList();

    Map<String, List<Homes>> finalValue = new HashMap<>();
    finalValue.put("new", newHomes);
    finalValue.put("deleted", deletedHomes);
    return finalValue;
  }


  private Set<String> getDeletedObjects(List<Homes> preferredHomes) {
    return preferredHomes.stream().filter(home -> home.getEndPeriodMP().before(new Date()))
        .map(Homes::getObjectNo).collect(toSet());
  }

  private Set<String> getLastObjects() {
    return lastUpdatedRepository.findById(1)
        .map(LastUpdated::getPreferredObjects) // This is expected to be a List<String>
        .map(HashSet::new) // Convert List<String> to HashSet<String>
        .orElseGet(HashSet::new); // Return an empty HashSet if not found
  }

  private List<Homes> getNewPreferredHomes(HomeSearchConfig config) {
    Date date = new Date();
    int queuePoints = getQueuePoints(config.queuePoints(), config.queuePointsDate());
    List<Homes> preferredHomes = homesRepository.findAllByRentPerMonthSortBetweenAndObjectAreaSortBetweenAndQueuePointsLessThanAndObjectSubGroupNoBetweenAndMarketPlaceNoIsNotAndCompanyNoIsAndEndPeriodMPAfter(
        config.minRent(), config.maxRent(), config.minArea(), config.maxArea(), queuePoints, config.minRooms(), config.maxRooms(),
        config.marketplace(), config.company(), date);
    preferredHomes = preferredHomes.stream().filter(h -> h.getPlaceName().equalsIgnoreCase("Växjö") || h.getPlaceName().equalsIgnoreCase("Vxj")).toList();
    return preferredHomes;
  }

  private int getQueuePoints(int baseQueuePoints, int queuePointsDate) {
    LocalDate today = LocalDate.now();
    LocalDate qPointsDate = getPointsDate(queuePointsDate);
    int days = (int) (today.toEpochDay() - qPointsDate.toEpochDay());
    return baseQueuePoints + days;
  }

  private String getHomeMessage(Homes h) {
    return h.getStreet()
        + " | "
        + this.appConfig.getVxPrefixLink() + h.getObjectNo();
  }
}
