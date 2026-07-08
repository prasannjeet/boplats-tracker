package com.prasannjeet.vaxjobostader.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.prasannjeet.vaxjobostader.util.StaticUtils.getMapper;

@RequiredArgsConstructor
@Slf4j
public class SlackClient {

  private static final ObjectMapper mapper = getMapper();
  private final String slackUrl;

  public void sendSlackMessage(String message) throws IOException, InterruptedException {
    HttpClient client = HttpClient.newHttpClient();
    HttpRequest request = HttpRequest.newBuilder(URI.create(slackUrl))
        .header("Content-Type", "application/json")
        .POST(HttpRequest.BodyPublishers.ofString(getBody(message)))
        .build();
    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

    log.info("Slack Message Sent. Response: {}", response.body());

    if (!response.body().contains("ok")) {
      throw new RuntimeException(
          "Failed response received from slack. Response: " + response.body());
    }
  }

  String getBody(String message) {
    return mapper.createObjectNode()
        .put("text", message)
        .toString();
  }

}
