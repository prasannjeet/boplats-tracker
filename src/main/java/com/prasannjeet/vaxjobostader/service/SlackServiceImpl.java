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
import java.util.stream.Collectors;

import com.prasannjeet.vaxjobostader.client.SlackClient;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.enums.MarketPlaceDescription;
import com.prasannjeet.vaxjobostader.enums.PlaceName;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomes;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomesRepository;
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
  private final UserSelectedHomesRepository userSelectedHomesRepository;
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

    // Retrieve the last updated record or create it if it doesn't exist
    UserSelectedHomes userSelectedHomes = userSelectedHomesRepository.findById(config.name()).orElseGet(() -> {
      UserSelectedHomes newUserSelectedHomes = new UserSelectedHomes();
      newUserSelectedHomes.setId(config.name()); // Set the ID to the config name
      newUserSelectedHomes.setPreferredObjects(new ArrayList<>()); // Initialize with an empty list
      return newUserSelectedHomes;
    });

    // Use the retrieved last updated object numbers, ensuring it's not null
    Set<String> lastObjects = userSelectedHomes.getPreferredObjects() != null ?
        new HashSet<>(userSelectedHomes.getPreferredObjects()) : new HashSet<>();

    SetView<String> newObjects = difference(preferredHomesObjectNo, lastObjects);
    Set<String> deletedObjects = getDeletedObjects(preferredHomes);

    // Update the last updated record with the new set of preferred objects
    userSelectedHomes.setPreferredObjects(new ArrayList<>(preferredHomesObjectNo));
    userSelectedHomesRepository.save(userSelectedHomes);

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

  private List<Homes> getNewPreferredHomes(HomeSearchConfig config) {
    Date date = new Date();
    int queuePoints = getQueuePoints(config.queuePoints(), config.queuePointsDate());
    List<Homes> preferredHomes = homesRepository.findAllByRentPerMonthSortBetweenAndObjectAreaSortBetweenAndQueuePointsLessThanAndObjectSubGroupNoBetweenAndMarketPlaceNoIsNotAndCompanyNoIsAndEndPeriodMPAfter(
        config.minRent(), config.maxRent(), config.minArea(), config.maxArea(), queuePoints, config.minRooms(), config.maxRooms(),
        config.marketplace(), config.company(), date);

    // Filter homes based on the place names and marketplace descriptions from the config
    preferredHomes = filterHomesByPlaceNames(preferredHomes, config.placeNames());
    preferredHomes = filterHomesByMarketPlaceDescriptions(preferredHomes, config.marketPlaceDescriptions());
    return preferredHomes;
  }

  private List<Homes> filterHomesByPlaceNames(List<Homes> homes, Set<PlaceName> placeNames) {
    Set<String> placeNameDisplayNames = placeNames.stream()
        .map(PlaceName::getDisplayName)
        .collect(Collectors.toSet());

    return homes.stream()
        .filter(home -> placeNameDisplayNames.contains(home.getPlaceName()))
        .collect(toList());
  }

  private List<Homes> filterHomesByMarketPlaceDescriptions(List<Homes> homes, Set<MarketPlaceDescription> marketPlaceDescriptions) {
    Set<String> marketPlaceDescriptionNames = marketPlaceDescriptions.stream()
        .map(MarketPlaceDescription::getDescription)
        .collect(toSet());

    return homes.stream()
        .filter(home -> marketPlaceDescriptionNames.contains(home.getMarketPlaceDescription()))
        .collect(toList());
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
