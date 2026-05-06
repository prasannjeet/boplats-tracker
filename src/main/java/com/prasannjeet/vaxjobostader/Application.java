package com.prasannjeet.vaxjobostader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@Slf4j
public class Application {

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
}
