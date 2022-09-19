package com.prasannjeet.vaxjobostader.client;

import static com.prasannjeet.vaxjobostader.util.StaticUtils.getMapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.dto.request.RequestRoot;
import com.prasannjeet.vaxjobostader.client.dto.response.ResponseRoot;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.methods.PostMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class VaxjobostaderClient {

  private static final Logger LOG = LoggerFactory.getLogger(VaxjobostaderClient.class);
  private final ObjectMapper mapper;
  private final URI hostUri;
  private final HttpClient httpClient;

  public VaxjobostaderClient(AppConfig config) throws IOException {
    this.mapper = getMapper();
    this.hostUri = URI.create(config.getVbUrl());

    this.httpClient = new HttpClient();
    PostMethod mPost = new PostMethod(this.hostUri.toString());
    this.httpClient.executeMethod( mPost );
    LOG.info("VaxjobostaderClient initialized");
  }

  Header getHeader() {
    Header mtHeader = new Header();
    mtHeader.setName("Content-Type");
    mtHeader.setValue("application/x-www-form-urlencoded");
    mtHeader.setName("Accept");
    mtHeader.setValue("application/json, text/javascript, */*");
    return mtHeader;
  }

  NameValuePair[] getPostBody(RequestRoot root) throws JsonProcessingException {
    NameValuePair[] body = new NameValuePair[4];
    body[0] = new NameValuePair("Parm1", mapper.writeValueAsString(root));
    body[1] = new NameValuePair("CallbackMethod", "PostObjectSearch");
    body[2] = new NameValuePair("CallbackParmCount", "1");
    body[3] = new NameValuePair("__WWEVENTCALLBACK", "");
    return body;
  }

  public ResponseRoot getResponseHttpClient(RequestRoot root)
      throws IOException {
    PostMethod mPost = new PostMethod(this.hostUri.toString());
    mPost.addRequestHeader(getHeader());
    mPost.setRequestBody(getPostBody(root));
    this.httpClient.executeMethod(mPost);
    InputStream responseBody = mPost.getResponseBodyAsStream();
    return mapper.readValue(responseBody, ResponseRoot.class);
  }

}
