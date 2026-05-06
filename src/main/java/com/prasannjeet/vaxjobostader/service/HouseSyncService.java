package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseDetail;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListResponse;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseSyncService {

    private final VaxjobostaderClient vaxjobostaderClient;
    private final HouseRepository houseRepository;
    private final AppConfig appConfig;

    /**
     * Fetches the full property listing and reconciles it with the database.
     * Runs every N hours (default 24). Does NOT touch detail fields (queue points,
     * application deadline, image) — those are managed by syncHouseDetails().
     */
    @Scheduled(fixedDelayString = "${appconfig.listApiCacheDurationHours:24}", timeUnit = TimeUnit.HOURS)
    @Transactional
    public void syncHouseList() {
        log.info("Starting house list synchronization...");
        try {
            HouseListResponse response = vaxjobostaderClient.getPropertiesList();
            if (response == null || response.items() == null || response.items().isEmpty()) {
                log.warn("Received empty or null list response from API. Aborting sync.");
                return;
            }

            List<HouseListItem> apiItems = response.items();
            Set<String> apiIds = apiItems.stream()
                .map(HouseListItem::id)
                .collect(Collectors.toSet());

            // Load all existing houses as a map for O(1) lookup
            Map<String, House> existingMap = houseRepository.findAll().stream()
                .collect(Collectors.toMap(House::getId, h -> h));

            List<House> toUpsert = new ArrayList<>(apiItems.size());

            for (HouseListItem item : apiItems) {
                House house = existingMap.getOrDefault(item.id(), new House());
                boolean isNew = house.getId() == null;

                house.setId(item.id());
                house.setLocalId(item.localId());
                house.setDescription(item.description());
                house.setEndDate(null); // ensure active

                // Only set area-level address for new houses; detail sync will overwrite with full address
                if (isNew && item.location() != null && item.location().area() != null) {
                    house.setAddress(item.location().area().displayName());
                }

                if (item.pricing() != null) {
                    house.setRent(item.pricing().price());
                }
                if (item.size() != null) {
                    house.setArea(item.size().area());
                    house.setRooms(parseRooms(item.size().shortRoomsDisplayName()));
                }
                if (item.availability() != null) {
                    house.setAvailableFrom(item.availability().availableFrom());
                }

                toUpsert.add(house);
            }

            houseRepository.saveAll(toUpsert);

            // Mark houses that disappeared from the listing as ended
            List<House> toEnd = existingMap.values().stream()
                .filter(h -> !apiIds.contains(h.getId()) && h.getEndDate() == null)
                .peek(h -> h.setEndDate(new Date()))
                .collect(Collectors.toList());

            if (!toEnd.isEmpty()) {
                houseRepository.saveAll(toEnd);
            }

            log.info("House list sync complete. Upserted: {}, marked ended: {}",
                toUpsert.size(), toEnd.size());

        } catch (Exception e) {
            log.error("Error during house list synchronization", e);
        }
    }

    /**
     * Processes a batch of houses that need their details refreshed.
     * A house needs a refresh if it has never been fetched (lastDetailFetchedAt IS NULL)
     * or if its last fetch is older than detailRefreshIntervalHours.
     * Runs every N seconds (default 60). Processes detailSyncBatchSize houses per tick.
     *
     * This design ensures queue points (queuePointsCurrentPositionX), which can appear
     * days after a listing goes live, are always eventually captured.
     */
    @Scheduled(fixedDelayString = "${appconfig.detailApiCallIntervalSeconds:60}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void syncHouseDetails() {
        Instant cutoff = Instant.now().minus(appConfig.getDetailRefreshIntervalHours(), ChronoUnit.HOURS);
        int batchSize = appConfig.getDetailSyncBatchSize();

        List<House> batch = houseRepository.findHousesNeedingDetailRefresh(
            cutoff, PageRequest.of(0, batchSize)
        );

        if (batch.isEmpty()) {
            log.debug("No houses needing detail refresh.");
            return;
        }

        log.info("Refreshing details for {} house(s).", batch.size());

        for (House house : batch) {
            Instant fetchedAt = Instant.now();
            try {
                HouseDetail detail = vaxjobostaderClient.getPropertyDetail(house.getId());
                if (detail == null) {
                    log.warn("Null detail response for house {}. Skipping.", house.getId());
                    house.setLastDetailFetchedAt(fetchedAt);
                    houseRepository.save(house);
                    continue;
                }

                applyDetailToHouse(house, detail);
                house.setLastDetailFetchedAt(fetchedAt);
                houseRepository.save(house);

                log.debug("Detail refreshed for house {}", house.getId());

            } catch (Exception e) {
                log.error("Failed to fetch detail for house {}. Will retry next cycle.", house.getId(), e);
                // Mark as attempted so this house doesn't block the queue; it will be retried next cycle
                house.setLastDetailFetchedAt(fetchedAt);
                houseRepository.save(house);
            }
        }
    }

    private void applyDetailToHouse(House house, HouseDetail detail) {
        if (detail.location() != null && detail.location().address() != null) {
            String fullAddress = detail.location().address().completeAdress();
            if (fullAddress != null && !fullAddress.isBlank()) {
                house.setAddress(fullAddress);
            }
        }
        if (detail.application() != null) {
            house.setApplicationDeadline(detail.application().openTo());
        }
        if (detail.files() != null
                && detail.files().locationImage() != null
                && !detail.files().locationImage().isEmpty()) {
            house.setImageUrl(detail.files().locationImage().get(0).address());
        }
        // Store queue points even when null — null is valid data meaning no cutoff established yet
        house.setQueuePoints(detail.queuePointsCurrentPositionX());
    }

    private Integer parseRooms(String shortRoomsDisplayName) {
        if (shortRoomsDisplayName == null || shortRoomsDisplayName.isBlank()) return null;
        String digits = shortRoomsDisplayName.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) return null;
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
