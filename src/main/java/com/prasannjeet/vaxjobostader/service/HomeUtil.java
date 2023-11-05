package com.prasannjeet.vaxjobostader.service;

import static lombok.AccessLevel.PRIVATE;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.prasannjeet.vaxjobostader.enums.MarketPlaceDescription;
import com.prasannjeet.vaxjobostader.enums.PlaceName;
import com.prasannjeet.vaxjobostader.jpa.Homes;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = PRIVATE)
public class HomeUtil {

    //TODO: Filter by market place no (not description) and fetch directly from sql
    public static List<Homes> filterHomes(List<Homes> homes, HomeSearchConfig config) {
        Set<String> placeNameDisplayNames = config.placeNames().stream()
            .map(PlaceName::getDisplayName)
            .collect(Collectors.toSet());

        Set<String> marketPlaceDescriptionNames = config.marketPlaceDescriptions().stream()
            .map(MarketPlaceDescription::getDescription)
            .collect(Collectors.toSet());

        return homes.stream()
            .filter(home -> placeNameDisplayNames.contains(home.getPlaceName()))
            .filter(home -> marketPlaceDescriptionNames.contains(home.getMarketPlaceDescription()))
            .toList();
    }
}
