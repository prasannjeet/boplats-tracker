package com.prasannjeet.vaxjobostader.legacy;

import com.prasannjeet.vaxjobostader.enums.PlaceName;

import java.util.Set;

public record HomeSearchConfig(
    String name,
    String webHook,
    int minRent,
    int maxRent,
    int minArea,
    int maxArea,
    int queuePoints,
    int queuePointsDate,
    int minRooms,
    int maxRooms,
    int marketplace,
    int company,
    Set<MarketPlaceDescription> marketPlaceDescriptions,
    Set<PlaceName> placeNames,
    boolean needLastDateNotification) {
}
