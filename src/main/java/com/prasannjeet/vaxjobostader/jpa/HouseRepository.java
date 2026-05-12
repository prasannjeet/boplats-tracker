package com.prasannjeet.vaxjobostader.jpa;

import com.prasannjeet.vaxjobostader.legacy.HomeSearchConfig;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface HouseRepository extends JpaRepository<House, Long> {

    List<House> findAllByEndDateIsNull();

    Optional<House> findByIdAndAvailableFromAndEndDateIsNull(String id, Date availableFrom);

    long countByEndDateIsNull();

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

    /**
     * Internal IDs of houses that have an address but no coordinates yet.
     * Used by the one-shot geocode backfill to catch rows that were
     * inserted before geocoding was wired into the sync.
     */
    @Query("""
        SELECT h.internalId FROM House h
        WHERE h.completeAddress IS NOT NULL
        AND h.completeAddress <> ''
        AND (h.latitude IS NULL OR h.longitude IS NULL)
        """)
    List<Long> findInternalIdsNeedingGeocode();

    @Modifying
    @Query("""
        UPDATE House h
        SET h.endDate = :endDate
        WHERE h.endDate IS NULL
        AND h.applicationDeadline IS NOT NULL
        AND h.applicationDeadline < :now
        """)
    int markPastDeadlineEnded(@Param("endDate") LocalDate endDate, @Param("now") Date now);

    default List<House> findPreferredHomes(HomeSearchConfig config) {
        return findAllByRentBetweenAndAreaBetweenAndRoomsBetweenAndEndDateIsNull(
            (double) config.minRent(), (double) config.maxRent(),
            (double) config.minArea(), (double) config.maxArea(),
            config.minRooms(), config.maxRooms()
        );
    }
}
