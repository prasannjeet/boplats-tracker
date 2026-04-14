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
  private Integer listApiCacheDurationHours;
  private Integer detailApiCallIntervalSeconds;
  private String slackWebhookUrl;
  private String lastDateCron;
  private String slackCron;
  private String vxPrefixLink;
  private String configFilePath;

}
