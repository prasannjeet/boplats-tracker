package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseThumbnail(boolean exists, String displayName, String version) {}
