package com.prasannjeet.vaxjobostader.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HouseRepository extends JpaRepository<House, String> {

    // Find all houses that do not have an end date set
    List<House> findAllByEndDateIsNull();

    // Find one house that needs its details fetched
    House findFirstByEndDateIsNullAndApplicationDeadlineIsNull();

    List<House> findAllByRentBetweenAndAreaBetweenAndRoomsBetweenAndEndDateIsNull(
        Double minRent, Double maxRent, Double minArea, Double maxArea, Integer minRooms, Integer maxRooms
    );

    default List<House> findPreferredHomes(com.prasannjeet.vaxjobostader.service.preferences.HomeSearchConfig config) {
        return findAllByRentBetweenAndAreaBetweenAndRoomsBetweenAndEndDateIsNull(
            (double) config.minRent(), (double) config.maxRent(),
            (double) config.minArea(), (double) config.maxArea(),
            config.minRooms(), config.maxRooms()
        );
    }
}
