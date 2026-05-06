package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.client.SlackClient;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomes;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomesRepository;
import com.prasannjeet.vaxjobostader.legacy.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;

import static com.prasannjeet.vaxjobostader.service.HomeUtil.filterHomes;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.SetUtils.difference;

@RequiredArgsConstructor
@Transactional
@Slf4j
public class SlackServiceImpl implements SlackService {

  private final HouseRepository houseRepository;
  private final UserSelectedHomesRepository userSelectedHomesRepository;
  private final AppConfig appConfig;

  @Override
  @SneakyThrows
  public void syncPreferredHomes(HomeSearchConfig config) {
    log.info("Checking for new Homes for Slack Notification at: {}", now());
    Map<String, List<House>> homesUpdates = getHomesUpdates(config);

    String message = buildSlackMessage(homesUpdates);
    if (!homesUpdates.get("new").isEmpty()) {
      sendSlackNotification(config.webHook(), message);
    }
  }

  @Override
  @SneakyThrows
  public void sendNotificationOfLastDayToApplyForHomes(List<House> homes, HomeSearchConfig config, boolean forceSend) {
    String message = "Today is the last day to apply for the following homes. Total";
    StringBuilder sb = new StringBuilder();
    appendHomesUpdateMessage(sb, message, homes);
    if (forceSend || !homes.isEmpty()) {
      sendSlackNotification(config.webHook(), sb.toString());
    }
  }

  private List<House> getCurrentPreferredHomes(HomeSearchConfig config) {
    List<House> preferredHomes = houseRepository.findPreferredHomes(config);
    return filterHomes(preferredHomes, config);
  }

  private Map<String, List<House>> getHomesUpdates(HomeSearchConfig config) {
    // Get all homes based on user preference
    List<House> allPreferredHomes = getCurrentPreferredHomes(config);

    // Get homes base on user preference from last time
    UserSelectedHomes userSelectedHomes = getUserSelectedHomes(config);

    // Find out new homes for the user (homes that are not in the user's last preference)
    Set<String> newPreferredHomes = getNewHomesObjectNos(allPreferredHomes, userSelectedHomes);

    updatePreferredHomesRecord(userSelectedHomes, allPreferredHomes);
    return mapHomesUpdates(allPreferredHomes, newPreferredHomes);
  }

  private UserSelectedHomes getUserSelectedHomes(HomeSearchConfig config) {
    return userSelectedHomesRepository.findById(config.name())
        .orElseGet(() -> createUserSelectedHomesRecord(config));
  }

  private UserSelectedHomes createUserSelectedHomesRecord(HomeSearchConfig config) {
    UserSelectedHomes newUserSelectedHomes = new UserSelectedHomes();
    newUserSelectedHomes.setId(config.name());
    newUserSelectedHomes.setPreferredObjects(new ArrayList<>());
    return newUserSelectedHomes;
  }

  private Set<String> getNewHomesObjectNos(List<House> preferredHomes, UserSelectedHomes userSelectedHomes) {
    Set<String> preferredHomesObjectNos = getObjectNos(preferredHomes);
    Set<String> lastObjects = new HashSet<>(userSelectedHomes.getPreferredObjects());
    return difference(preferredHomesObjectNos, lastObjects).toSet();
  }

  private void updatePreferredHomesRecord(UserSelectedHomes userSelectedHomes, List<House> currentPreferredHomes) {
    userSelectedHomes.setPreferredObjects(getObjectNos(currentPreferredHomes).stream().toList());
    userSelectedHomesRepository.save(userSelectedHomes);
  }

  // Get full Home Objects for new and deleted homes based on String ids
  private Map<String, List<House>> mapHomesUpdates(List<House> allHomes, Set<String> newHomesObjectNos) {
    Map<String, List<House>> updates = new HashMap<>();
    updates.put("new", filterHomesByObjectNos(allHomes, newHomesObjectNos));
    return updates;
  }

  private List<House> filterHomesByObjectNos(List<House> homes, Set<String> objectNos) {
    return homes.stream()
        .filter(home -> objectNos.contains(home.getId()))
        .toList();
  }

  private Set<String> getObjectNos(List<House> homes) {
    return homes.stream()
        .map(House::getId)
        .collect(toSet());
  }

  private String buildSlackMessage(Map<String, List<House>> homesUpdates) {
    StringBuilder sb = new StringBuilder();
    appendHomesUpdateMessage(sb, "New Homes", homesUpdates.get("new"));
    return sb.toString();
  }

  private void appendHomesUpdateMessage(StringBuilder sb, String title, List<House> homes) {
    sb.append(title).append(": ").append(homes.size()).append("\n");
    homes.forEach(home -> sb.append(getHomeMessage(home)).append("\n"));
  }

  private void sendSlackNotification(String webHook, String message) throws IOException {
    SlackClient slackClient = new SlackClient(webHook);
    slackClient.sendSlackMessage(message);
    log.info("Slack message sent. Message: {}", message);
  }

  private String getHomeMessage(House home) {
    // Try to construct a useful URL using vxPrefixLink
    // If not possible, just display ID.
    return (home.getAddress() != null ? home.getAddress() : "Unknown Address") + " | " + appConfig.getVxPrefixLink() + home.getId();
  }

}