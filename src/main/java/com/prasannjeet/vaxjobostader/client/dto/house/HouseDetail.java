package com.prasannjeet.vaxjobostader.client.dto.house;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseDetail(
    String id,
    String localId,
    String number,
    String displayName,
    String type,
    HousePricing pricing,
    HouseLocation location,
    HouseAvailability availability,
    HouseApplication application,
    HouseSize size,
    HouseFiles files
) {}