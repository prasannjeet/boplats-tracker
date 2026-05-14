package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseListItem(
    String id,
    String localId,
    String displayName,
    String description,
    String type,
    String queueType,
    String rentalObjectType,
    HousePricing pricing,
    HouseLocation location,
    HouseAvailability availability,
    HouseSize size,
    HouseThumbnail thumbnail
) {}