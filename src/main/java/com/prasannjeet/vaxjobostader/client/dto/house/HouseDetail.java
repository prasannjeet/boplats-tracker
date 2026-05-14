package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseDetail(
    String id,
    String localId,
    String number,
    String displayName,
    String type,
    String queueType,
    String queueTypeDisplayName,
    String rentalObjectType,
    Integer nrApplications,
    HousePricing pricing,
    HouseLocation location,
    HouseAvailability availability,
    HouseApplication application,
    HouseSize size,
    HouseFiles files,
    Double queuePointsCurrentPositionX,
    List<HouseIncluded> included
) {}
