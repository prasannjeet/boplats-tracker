package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ObjectTypeMetadata(
    String displayName,
    String description,
    Integer maxNumberOfApplications,
    Integer minNumberOfRooms,
    Integer maxNumberOfRooms,
    Double minSize,
    Double maxSize,
    Double minPrice,
    Double maxPrice,
    Integer numberOfMarketObjects
) {}
