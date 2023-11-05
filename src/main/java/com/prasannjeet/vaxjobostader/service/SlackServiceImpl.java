package com.prasannjeet.vaxjobostader.service;

import static com.prasannjeet.vaxjobostader.service.HomeUtil.filterHomes;
import static java.time.LocalDateTime.now;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.SetUtils.difference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.prasannjeet.vaxjobostader.client.SlackClient;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomes;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomesRepository;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional
@Slf4j
public class SlackServiceImpl implements SlackService {

  private final HomesRepository homesRepository;
  private final UserSelectedHomesRepository userSelectedHomesRepository;
  private final AppConfig appConfig;

  @Override
  @SneakyThrows
  public void syncPreferredHomes(HomeSearchConfig config) {
    log.info("Checking for new Homes for Slack Notification at: {}", now());
    Map<String, List<Homes>> homesUpdates = getHomesUpdates(config);

    String message = buildSlackMessage(homesUpdates);
    sendSlackNotification(config.webHook(), message);
  }

  @Override
  @SneakyThrows
  public void sendNotificationOfLastDayToApplyForHomes(List<Homes> homes, HomeSearchConfig config, boolean forceSend) {
    String message = "Today is the last day to apply for the following homes. Total";
    StringBuilder sb = new StringBuilder();
    appendHomesUpdateMessage(sb, message, homes);
    if (forceSend || !homes.isEmpty()) {
      sendSlackNotification(config.webHook(), sb.toString());
    }
  }

  private List<Homes> getCurrentPreferredHomes(HomeSearchConfig config) {
    List<Homes> preferredHomes = homesRepository.findPreferredHomes(config);
    return filterHomes(preferredHomes, config);
  }

  private Map<String, List<Homes>> getHomesUpdates(HomeSearchConfig config) {
    // Get all homes based on user preference
    List<Homes> allPreferredHomes = getCurrentPreferredHomes(config);

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

  private Set<String> getNewHomesObjectNos(List<Homes> preferredHomes, UserSelectedHomes userSelectedHomes) {
    Set<String> preferredHomesObjectNos = getObjectNos(preferredHomes);
    Set<String> lastObjects = new HashSet<>(userSelectedHomes.getPreferredObjects());
    return difference(preferredHomesObjectNos, lastObjects).toSet();
  }

  private void updatePreferredHomesRecord(UserSelectedHomes userSelectedHomes, List<Homes> currentPreferredHomes) {
    userSelectedHomes.setPreferredObjects(getObjectNos(currentPreferredHomes).stream().toList());
    userSelectedHomesRepository.save(userSelectedHomes);
  }

  // Get full Home Objects for new and deleted homes based on String ids
  private Map<String, List<Homes>> mapHomesUpdates(List<Homes> allHomes, Set<String> newHomesObjectNos) {
    Map<String, List<Homes>> updates = new HashMap<>();
    updates.put("new", filterHomesByObjectNos(allHomes, newHomesObjectNos));
    return updates;
  }

  private List<Homes> filterHomesByObjectNos(List<Homes> homes, Set<String> objectNos) {
    return homes.stream()
        .filter(home -> objectNos.contains(home.getObjectNo()))
        .toList();
  }

  private Set<String> getObjectNos(List<Homes> homes) {
    return homes.stream()
        .map(Homes::getObjectNo)
        .collect(toSet());
  }

  private String buildSlackMessage(Map<String, List<Homes>> homesUpdates) {
    StringBuilder sb = new StringBuilder();
    appendHomesUpdateMessage(sb, "New Homes", homesUpdates.get("new"));
    return sb.toString();
  }

  private void appendHomesUpdateMessage(StringBuilder sb, String title, List<Homes> homes) {
    sb.append(title).append(": ").append(homes.size()).append("\n");
    homes.forEach(home -> sb.append(getHomeMessage(home)).append("\n"));
  }

  private void sendSlackNotification(String webHook, String message) throws IOException {
    SlackClient slackClient = new SlackClient(webHook);
    slackClient.sendSlackMessage(message);
    log.info("Slack message sent. Message: {}", message);
  }

  private String getHomeMessage(Homes home) {
    return home.getStreet() + " | " + appConfig.getVxPrefixLink() + home.getObjectNo();
  }

}