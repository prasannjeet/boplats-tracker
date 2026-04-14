package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.enums.PlaceName;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class HomeUtil {

    public static List<House> filterHomes(List<House> houses, HomeSearchConfig config) {
        Set<String> placeNameDisplayNames = config.placeNames().stream()
            .map(PlaceName::getDisplayName)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());

        return houses.stream()
            .filter(house -> {
                if (placeNameDisplayNames.isEmpty()) return true; // If no place names specified, don't filter
                if (house.getAddress() == null) return false;
                
                // If any of the place names are found in the address (case insensitive)
                String addressLower = house.getAddress().toLowerCase();
                return placeNameDisplayNames.stream().anyMatch(addressLower::contains);
            })
            .toList();
    }
}
