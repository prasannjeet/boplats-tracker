package com.prasannjeet.vaxjobostader.config;

import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@EnableConfigurationProperties
@ConfigurationProperties("appconfig")
public class AppConfig {

  private static final Logger LOG = LoggerFactory.getLogger(AppConfig.class);
  private String vbUrl;
  private String slackUrl;
  private String databaseUpdateCron;
  private String slackCron;

}
