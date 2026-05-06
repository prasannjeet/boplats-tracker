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

}
