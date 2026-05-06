package com.prasannjeet.vaxjobostader.jpa;

import com.prasannjeet.vaxjobostader.legacy.HomeSearchConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface HouseRepository extends JpaRepository<House, String> {

    List<House> findAllByEndDateIsNull();

    List<House> findAllByRentBetweenAndAreaBetweenAndRoomsBetweenAndEndDateIsNull(
        Double minRent, Double maxRent, Double minArea, Double maxArea, Integer minRooms, Integer maxRooms
    );

    @Query("""
        SELECT h FROM House h
        WHERE h.endDate IS NULL
        AND (h.lastDetailFetchedAt IS NULL OR h.lastDetailFetchedAt < :cutoff)
        ORDER BY h.lastDetailFetchedAt ASC NULLS FIRST
        """)
    List<House> findHousesNeedingDetailRefresh(@Param("cutoff") Instant cutoff, Pageable pageable);

    default List<House> findPreferredHomes(HomeSearchConfig config) {
        return findAllByRentBetweenAndAreaBetweenAndRoomsBetweenAndEndDateIsNull(
            (double) config.minRent(), (double) config.maxRent(),
            (double) config.minArea(), (double) config.maxArea(),
            config.minRooms(), config.maxRooms()
        );
    }
}
