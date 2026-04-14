package com.prasannjeet.vaxjobostader.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseDetail;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListResponse;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.util.StaticUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
@Slf4j
public class VaxjobostaderClient {

  private final ObjectMapper mapper;
  private final URI hostUri;
  private final String apiKey;
  private final HttpClient httpClient;

  public VaxjobostaderClient(AppConfig config) {
    this.mapper = StaticUtils.getMapper();
    this.hostUri = URI.create(config.getVbUrl());
    this.apiKey = config.getVbApiKey();
    this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    log.info("VaxjobostaderClient initialized with URI: {}", this.hostUri);
  }

  private HttpRequest.Builder getBaseRequestBuilder(URI uri) {
    return HttpRequest.newBuilder()
            .uri(uri)
            .header("X-Api-Key", this.apiKey)
            .header("Accept", "application/json, text/plain, */*");
  }

  public HouseListResponse getPropertiesList() throws IOException, InterruptedException {
    // Append the query params for the list API
    URI listUri = URI.create(this.hostUri.toString() + "?type=residential&limit=1000");
    HttpRequest request = getBaseRequestBuilder(listUri).GET().build();

    HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
        log.error("Failed to fetch list. Status code: {}", response.statusCode());
        throw new IOException("Failed to fetch list from Momentum API");
    }

    return mapper.readValue(response.body(), HouseListResponse.class);
  }

  public HouseDetail getPropertyDetail(String id) throws IOException, InterruptedException {
    URI detailUri = URI.create(this.hostUri.toString() + "/" + id);
    HttpRequest request = getBaseRequestBuilder(detailUri).GET().build();

    HttpResponse<String> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
        log.error("Failed to fetch detail for ID: {}. Status code: {}", id, response.statusCode());
        throw new IOException("Failed to fetch detail from Momentum API");
    }

    return mapper.readValue(response.body(), HouseDetail.class);
  }
}
