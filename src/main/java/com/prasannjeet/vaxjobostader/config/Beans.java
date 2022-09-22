package com.prasannjeet.vaxjobostader.config;

import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.LastUpdatedRepository;
import com.prasannjeet.vaxjobostader.service.HomeService;
import com.prasannjeet.vaxjobostader.service.HomeServiceImpl;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.SlackServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class Beans {

  final
  VaxjobostaderClient client;

  final
  HomesRepository homesRepository;

  final
  LastUpdatedRepository lastUpdatedRepository;

  final
  AppConfig appConfig;

  @Bean
  public HomeService homeService() {
    return new HomeServiceImpl(client, homesRepository, lastUpdatedRepository);
  }

  @Bean
  public SlackService lastUpdatedService() {
    return new SlackServiceImpl(homesRepository, lastUpdatedRepository, appConfig);
  }

}
