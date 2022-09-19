package com.prasannjeet.vaxjobostader.client.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AreaLimit {

  @JsonProperty("Min")
  private int min;
  @JsonProperty("Max")
  private int max;

}
