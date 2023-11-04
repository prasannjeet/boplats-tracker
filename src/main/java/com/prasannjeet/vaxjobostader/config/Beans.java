package com.prasannjeet.vaxjobostader.config;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.jpa.HomesRepository;
import com.prasannjeet.vaxjobostader.jpa.LastUpdatedRepository;
import com.prasannjeet.vaxjobostader.service.HomeService;
import com.prasannjeet.vaxjobostader.service.HomeServiceImpl;
import com.prasannjeet.vaxjobostader.service.SlackService;
import com.prasannjeet.vaxjobostader.service.SlackServiceImpl;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

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


  @Bean
  public List<HomeSearchConfig> homeSearchConfigList() {
    ObjectMapper objectMapper = new ObjectMapper();
    ClassPathResource resource = new ClassPathResource("homeSearchConfig.json");
    try {
      return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<HomeSearchConfig>>() {});
    } catch (JsonParseException | JsonMappingException e) {
      // This means there's a problem with the JSON format
      throw new IllegalStateException("Failed to parse homeSearchConfig.json due to JSON formatting error: " + e.getMessage(), e);
    } catch (IOException e) {
      // This could mean the file doesn't exist or isn't readable
      throw new IllegalStateException("Failed to read homeSearchConfig.json, please check if the file exists and is readable: " + e.getMessage(), e);
    }
  }

}
