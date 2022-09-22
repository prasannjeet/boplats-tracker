package com.prasannjeet.vaxjobostader;

import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.service.HomeService;
import com.prasannjeet.vaxjobostader.service.SlackService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
public class Application {

  private static final Logger LOG = LoggerFactory.getLogger(Application.class);

  final VaxjobostaderClient client;
  final HomeService homeService;
  final SlackService slackService;
  final AppConfig appConfig;

  public static void main(String[] args) {
    while (true) {
      try {
        SpringApplication.run(Application.class, args);
        LOG.info("Application started");
        break;
      } catch (Exception e) {
        LOG.error("Startup Exception: {}", e.getMessage());
        LOG.error("Retrying in 5 seconds...");
        try {
          Thread.sleep(5000);
        } catch (InterruptedException e1) {
          Thread.currentThread().interrupt();
          e1.printStackTrace();
        }
      }
    }
  }

  @EventListener(ApplicationReadyEvent.class)
  public void runOnStartup() {
    try {
      LOG.info("Running one-time startup logic.");
      slackService.syncPreferredHomes();
    } catch (Exception e) {
      LOG.error("An error occurred while running startup method.", e);
    }
  }

  @Scheduled(cron = "${appconfig.databaseUpdateCron}")
  public void updateDatabase() {
    int size = 0;
    try {
      LOG.info("Running scheduled task");
      ResponseRoot response = homeService.getAllItemsFromApi();
      homeService.updateDatabase(response.getResult());
      size = response.getResult().size();
    } catch (Exception e) {
      LOG.error("Error updating homes: {}", e.getMessage(), e);
    }
    LOG.info("Scheduled task completed. Homes updated: {}", size);
  }

  @Scheduled(cron = "${appconfig.slackCron}")
  public void checkForNewItems() {
    try {
      slackService.syncPreferredHomes();
    } catch (Exception e) {
      LOG.error("An error occurred while comparing new and deleted items", e);
    }
  }

}
