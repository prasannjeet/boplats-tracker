package com.prasannjeet.vaxjobostader.service.preferences;

import java.util.Set;

import com.prasannjeet.vaxjobostader.enums.MarketPlaceDescription;
import com.prasannjeet.vaxjobostader.enums.PlaceName;

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
