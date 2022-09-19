package com.prasannjeet.vaxjobostader.service;

import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.SetUtils.difference;

import com.prasannjeet.vaxjobostader.client.SlackClient;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.LastUpdated;
import com.prasannjeet.vaxjobostader.jpa.LastUpdatedRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.collections4.SetUtils.SetView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
@Transactional
public class LastUpdatedServiceImpl implements LastUpdatedService {

  private static final Logger LOG = LoggerFactory.getLogger(LastUpdatedServiceImpl.class);
  private static final String LINK = "https://bpvx.vaxjo.se/pgObjectInformation.aspx?company=1&obj=";

  private static final int MIN_RENT = 0;
  private static final int MAX_RENT = 6000;
  private static final int MIN_AREA = 30;
  private static final int MAX_AREA = 155;
  private static final int Q_POINTS = 1923;
  private static final int Q_POINTS_DATE = 20220918;
  private static final int MIN_ROOMS = 0;
  private static final int MAX_ROOMS = 6;
  private static final int MARKETPLACE = 9;
  private static final int COMPANY = 1;

  private final HomesRepository homesRepository;
  private final LastUpdatedRepository lastUpdatedRepository;
  private final AppConfig appConfig;

  @SneakyThrows
  @Override
  public void syncPreferredHomes() {
    LOG.info("Checking for new Homes for Slack Notification at: {}", now());
    Map<String, List<Homes>> items = getNewHomes();

    List<Homes> newHomes = items.get("new");
    List<Homes> deletedHomes = items.get("deleted");
    StringBuilder sb = new StringBuilder();
    sb.append("New Homes: ").append(newHomes.size()).append("\n");
    newHomes.forEach(n -> sb.append(getHomeMessage(n)).append("\n"));
    sb.append("Deleted Homes: ").append(deletedHomes.size()).append("\n");
    deletedHomes.forEach(d -> sb.append(getHomeMessage(d)).append("\n"));

    SlackClient slackClient = new SlackClient(appConfig.getSlackUrl());
    slackClient.sendSlackMessage(sb.toString());
    LOG.info("Slack message sent. Message: {}", sb);
  }

  @Override
  public Map<String, List<Homes>> getNewHomes() {
    List<Homes> preferredHomes = getNewPreferredHomes();
    Set<String> preferredHomesObjectNo = preferredHomes.stream().map(Homes::getObjectNo).collect(toSet());
    SetView<String> newObjects = difference(preferredHomesObjectNo, getLastObjects());
    SetView<String> deletedObjects = difference(getLastObjects(), preferredHomesObjectNo);

    LastUpdated referenceById = lastUpdatedRepository.getReferenceById(1);
    referenceById.setPreferredObjects(new ArrayList<>(preferredHomesObjectNo));
    lastUpdatedRepository.save(referenceById);

    List<Homes> newHomes = preferredHomes.stream()
        .filter(homes -> newObjects.contains(homes.getObjectNo())).collect(toList());
    List<Homes> deletedHomes = preferredHomes.stream()
        .filter(homes -> deletedObjects.contains(homes.getObjectNo())).collect(toList());

    Map<String, List<Homes>> finalValue = new HashMap<>();
    finalValue.put("new", newHomes);
    finalValue.put("deleted", deletedHomes);
    return finalValue;

  }


  private Set<String> getLastObjects() {
    LastUpdated referenceById = lastUpdatedRepository.getReferenceById(1);
    if (referenceById.getPreferredObjects() == null || referenceById.getPreferredObjects().isEmpty()) {
      return new HashSet<>();
    }
    return new HashSet<>(referenceById.getPreferredObjects());
  }


  private List<Homes> getNewPreferredHomes() {
    Date date = new Date();
    int queuePoints = getQueuePoints();
    return homesRepository.findAllByRentPerMonthSortBetweenAndObjectAreaSortBetweenAndQueuePointsLessThanAndObjectSubGroupNoBetweenAndMarketPlaceNoIsNotAndCompanyNoIsAndEndPeriodMPAfter(
        MIN_RENT, MAX_RENT, MIN_AREA, MAX_AREA, queuePoints, MIN_ROOMS, MAX_ROOMS,
        MARKETPLACE, COMPANY, date);
  }

  private int getQueuePoints() {
    LocalDate today = LocalDate.now();
    LocalDate qPointsDate = getPointsDate();
    int days = (int) (today.toEpochDay() - qPointsDate.toEpochDay());
    return Q_POINTS + days;
  }

  private static LocalDate getPointsDate() {
    int year = Q_POINTS_DATE / 10000;
    int month = (Q_POINTS_DATE % 10000) / 100;
    int day = Q_POINTS_DATE % 100;

    return LocalDate.of(year, month, day);
  }

  private String getHomeMessage(Homes h) {
    StringBuilder sb = new StringBuilder();
    sb.append(h.getStreet());
    sb.append(" | ");
    sb.append(LINK).append(h.getObjectNo());
    return sb.toString();
  }

}
