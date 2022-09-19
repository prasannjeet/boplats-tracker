package com.prasannjeet.vaxjobostader.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
public final class Advertisement {

  @JsonProperty("No")
  private final int no;

}
