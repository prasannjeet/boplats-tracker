package com.prasannjeet.vaxjobostader;

import java.util.List;

import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.service.HomeService;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@SpringBootApplication
@EnableScheduling
@EnableCaching
@RequiredArgsConstructor
@Slf4j
public class Application {

  final VaxjobostaderClient client;
  final HomeService homeService;
  final SlackService lastUpdatedService;
  final AppConfig appConfig;
  final List<HomeSearchConfig> homeSearchConfigList;

  public static void main(String[] args) {
    while (true) {
      try {
        SpringApplication.run(Application.class, args);
        log.info("Application started");
        break;
      } catch (Exception e) {
        log.error("Startup Exception: {}", e.getMessage());
        log.error("Retrying in 5 seconds...");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e1) {
          Thread.currentThread().interrupt();
          log.error("Thread interrupted while sleeping.", e1);
        }
      }
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    try {
      log.info("Running one-time startup logic.");

      if (!homeSearchConfigList.isEmpty()) {
        var firstConfig = homeSearchConfigList.get(0);
        lastUpdatedService.syncPreferredHomes(firstConfig);
      }

    } catch (Exception e) {
      log.error("An error occurred while running startup method.", e);
    }
  }

  @Scheduled(cron = "${appconfig.databaseUpdateCron}")
  public void updateDatabase() {
    int size = 0;
    try {
      log.info("Running scheduled task");
      ResponseRoot response = homeService.getAllItemsFromApi();
      homeService.updateDatabase(response.getResult());
      size = response.getResult().size();
    } catch (Exception e) {
      log.error("Error updating homes: {}", e.getMessage(), e);
    }
    log.info("Scheduled task completed. Homes updated: {}", size);
  }

  @Scheduled(cron = "${appconfig.slackCron}")
  public void checkForNewItems() {
    try {

      if (homeSearchConfigList.isEmpty()) {
        log.info("No search configs found. Skipping slack notification.");
      } else {
        log.info("Found {} search configs. Running slack notification.", homeSearchConfigList.size());
        for (var homeSearchConfig : homeSearchConfigList) {
          lastUpdatedService.syncPreferredHomes(homeSearchConfig);
        }
      }
    } catch (Exception e) {
      log.error("An error occurred while comparing new and deleted items", e);
    }
  }

}
