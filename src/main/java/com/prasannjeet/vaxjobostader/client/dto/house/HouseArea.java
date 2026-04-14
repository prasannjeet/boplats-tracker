package com.prasannjeet.vaxjobostader.client.dto.house;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseArea(String displayName) {}