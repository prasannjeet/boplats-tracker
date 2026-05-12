package com.prasannjeet.vaxjobostader.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("appconfig")
public class AppConfig {

    private String vbUrl;
    private String vbApiKey;
    private int listApiCacheDurationHours = 24;
    private int detailRefreshIntervalHours = 12;
    private int detailFetchMinDelaySeconds = 30;
    private int detailFetchMaxDelaySeconds = 1800;
    private int detailFetchIdleDelaySeconds = 1800;
    private String slackWebhookUrl;
    private String vxPrefixLink;

    private Geocoding geocoding = new Geocoding();

    @Data
    public static class Geocoding {
        private boolean enabled = true;
        private String baseUrl = "https://nominatim.openstreetmap.org";
        // Nominatim usage policy: every client MUST send a meaningful
        // User-Agent (ideally with a contact). Keep this configurable.
        private String userAgent = "boplats-tracker/1.0";
        // Minimum gap between provider calls. Nominatim allows ~1 req/s;
        // 1100ms gives a small safety margin.
        private long rateLimitMs = 1100;
    }
}
