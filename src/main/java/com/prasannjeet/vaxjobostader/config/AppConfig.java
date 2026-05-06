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
    private int detailApiCallIntervalSeconds = 60;
    private int detailRefreshIntervalHours = 12;
    private int detailSyncBatchSize = 5;
    private String slackWebhookUrl;
    private String vxPrefixLink;

}
