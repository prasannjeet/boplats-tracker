package com.prasannjeet.vaxjobostader.service.preferences;

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
    int company) {
}
