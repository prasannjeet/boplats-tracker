package com.prasannjeet.vaxjobostader.config;

import static com.prasannjeet.vaxjobostader.util.StaticUtils.getMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.enums.MarketPlaceDescription;
import com.prasannjeet.vaxjobostader.enums.PlaceName;
import com.prasannjeet.vaxjobostader.jpa.ConstantsRepository;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.UserSelectedHomesRepository;
import com.prasannjeet.vaxjobostader.service.HomeService;
import com.prasannjeet.vaxjobostader.service.HomeServiceImpl;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.SlackServiceImpl;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class Beans {

  final VaxjobostaderClient client;

  final HomesRepository homesRepository;

  final UserSelectedHomesRepository userSelectedHomesRepository;

  final ConstantsRepository constantsRepository;

  final
  AppConfig appConfig;

  @Bean
  public HomeService homeService() {
    return new HomeServiceImpl(client, homesRepository, constantsRepository);
  }

  @Bean
  public SlackService lastUpdatedService() {
    return new SlackServiceImpl(homesRepository, userSelectedHomesRepository, appConfig);
  }


  @Bean
  public List<HomeSearchConfig> homeSearchConfigList() {
    ClassPathResource resource = new ClassPathResource("homeSearchConfig.json");
    List<HomeSearchConfig> validConfigs = new ArrayList<>();
    try {
      JsonNode root = getMapper().readTree(resource.getInputStream());
      for (JsonNode node : root) {
        try {
          String name = node.get("name").asText();
          String webHook = node.get("webHook").asText();
          int minRent = node.get("minRent").asInt();
          int maxRent = node.get("maxRent").asInt();
          int minArea = node.get("minArea").asInt();
          int maxArea = node.get("maxArea").asInt();
          int queuePoints = node.get("queuePoints").asInt();
          int queuePointsDate = node.get("queuePointsDate").asInt();
          int minRooms = node.get("minRooms").asInt();
          int maxRooms = node.get("maxRooms").asInt();
          int marketplace = node.get("marketplace").asInt();
          int company = node.get("company").asInt();

          Set<MarketPlaceDescription> marketPlaceDescriptions = parseMarketPlaceDescriptions(node.get("marketPlaceDescription").asText());
          Set<PlaceName> placeNames = parsePlaceNames(node.get("placeNames").asText());

          boolean needLastDateNotification = node.get("needLastDateNotification").asBoolean();

          HomeSearchConfig config = new HomeSearchConfig(
              name, webHook, minRent, maxRent, minArea, maxArea, queuePoints,
              queuePointsDate, minRooms, maxRooms, marketplace, company,
              marketPlaceDescriptions, placeNames, needLastDateNotification);

          validConfigs.add(config);
        } catch (IllegalArgumentException e) {
          // Log and ignore the invalid config
          log.error("Failed to import config for {}", node.get("name").asText(), e);
        }
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read or parse homeSearchConfig.json: " + e.getMessage(), e);
    }

    // Use LinkedHashMap to preserve order and ensure uniqueness by name
    Map<String, HomeSearchConfig> uniqueConfigs = validConfigs.stream()
        .collect(Collectors.toMap(
            HomeSearchConfig::name, // key is the name
            config -> config,       // value is the config object
            (existing, replacement) -> existing, // in case of key collision, keep the existing
            LinkedHashMap::new      // use LinkedHashMap to preserve order
        ));

    // Return a new list containing only unique configurations
    return new ArrayList<>(uniqueConfigs.values());
  }

  private Set<MarketPlaceDescription> parseMarketPlaceDescriptions(String descriptions) {
    return Arrays.stream(descriptions.split(","))
        .map(String::trim)
        .map(String::toUpperCase)
        .map(MarketPlaceDescription::valueOf)
        .collect(Collectors.toSet());
  }

  private Set<PlaceName> parsePlaceNames(String names) {
    return Arrays.stream(names.split(","))
        .map(String::trim)
        .map(String::toUpperCase)
        .map(PlaceName::valueOf)
        .collect(Collectors.toSet());
  }

}
