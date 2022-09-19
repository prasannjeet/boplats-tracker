package com.prasannjeet.vaxjobostader.client;

import static com.prasannjeet.vaxjobostader.util.StaticUtils.getMapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RequiredArgsConstructor
public class SlackClient {

  private static final ObjectMapper mapper = getMapper();
  private static final Logger LOG = LoggerFactory.getLogger(SlackClient.class);
  private final String slackUrl;

  public void sendSlackMessage(String message) throws IOException {
      HttpClient client = new HttpClient();
      PostMethod post = new PostMethod(slackUrl);
      post.addRequestHeader(getHeader());
      post.setRequestBody(getBody(message));
      client.executeMethod(post);
      post.getResponseBodyAsString();

      LOG.info("Slack Message Sent. Response: {}", post.getResponseBodyAsString());

      if (!post.getResponseBodyAsString().contains("ok")) {
        throw new RuntimeException("Failed response received from slack. Response: " + post.getResponseBodyAsString());
      }
  }

  Header getHeader() {
    Header mtHeader = new Header();
    mtHeader.setName("Content-Type");
    mtHeader.setValue("application/json");
    return mtHeader;
  }

  String getBody(String message) {
    return mapper.createObjectNode()
        .put("text", message)
        .toString();
  }

}
