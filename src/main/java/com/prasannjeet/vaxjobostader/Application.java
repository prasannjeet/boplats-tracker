package com.prasannjeet.vaxjobostader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        // Startup failure is fatal on purpose: the container restart policy
        // (unless-stopped) handles retries with visibility, and a broken
        // configuration should crash loudly instead of looping silently.
        SpringApplication.run(Application.class, args);
    }
}
