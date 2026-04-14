package com.prasannjeet.vaxjobostader.service;

import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseDetail;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListResponse;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseSyncService {

    private final VaxjobostaderClient vaxjobostaderClient;
    private final HouseRepository houseRepository;

    @Scheduled(fixedDelayString = "${appconfig.listApiCacheDurationHours:24}", timeUnit = TimeUnit.HOURS)
    @Transactional
    public void syncHouseList() {
        log.info("Starting house list synchronization...");
        try {
            HouseListResponse response = vaxjobostaderClient.getPropertiesList();
            if (response == null || response.items() == null) {
                log.warn("Received empty response from list API.");
                return;
            }

            List<House> existingHouses = houseRepository.findAll();
            Set<String> newActiveIds = response.items().stream().map(HouseListItem::id).collect(Collectors.toSet());

            // Update existing or add new from API
            for (HouseListItem item : response.items()) {
                House house = existingHouses.stream().filter(h -> h.getId().equals(item.id())).findFirst().orElse(new House());
                house.setId(item.id());
                house.setLocalId(item.localId());
                if (item.location() != null && item.location().area() != null) {
                    house.setAddress(item.location().area().displayName()); // Temporary address mapping until detail API is called
                }
                house.setDescription(item.description());
                if (item.pricing() != null) house.setRent(item.pricing().price());
                if (item.size() != null) {
                    house.setArea(item.size().area());
                    String roomsDisplay = item.size().shortRoomsDisplayName();
                    if (roomsDisplay != null) {
                        try {
                            house.setRooms(Integer.parseInt(roomsDisplay.replaceAll("[^0-9]", "")));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                if (item.availability() != null) house.setAvailableFrom(item.availability().availableFrom());
                house.setEndDate(null); // Mark as active

                houseRepository.save(house);
            }

            // Mark missing as ended (stale data handling)
            for (House existingHouse : existingHouses) {
                if (!newActiveIds.contains(existingHouse.getId()) && existingHouse.getEndDate() == null) {
                    log.info("House {} is no longer in the list. Marking as ended.", existingHouse.getId());
                    existingHouse.setEndDate(new Date());
                    houseRepository.save(existingHouse);
                }
            }
            log.info("House list synchronization completed. Total active items: {}", newActiveIds.size());
        } catch (Exception e) {
            log.error("Error during house list synchronization", e);
        }
    }

    @Scheduled(fixedDelayString = "${appconfig.detailApiCallIntervalSeconds:60}", timeUnit = TimeUnit.SECONDS)
    @Transactional
    public void fetchNextHouseDetail() {
        log.info("Checking for houses needing details...");
        House houseToUpdate = houseRepository.findFirstByEndDateIsNullAndApplicationDeadlineIsNull();

        if (houseToUpdate == null) {
            log.info("All active houses have details populated. Nothing to do.");
            return;
        }

        log.info("Fetching details for house ID: {}", houseToUpdate.getId());
        try {
            HouseDetail detail = vaxjobostaderClient.getPropertyDetail(houseToUpdate.getId());
            if (detail == null) {
                log.warn("Received empty detail response for house {}", houseToUpdate.getId());
                return;
            }

            if (detail.application() != null) {
                houseToUpdate.setApplicationDeadline(detail.application().openTo());
            }

            if (detail.location() != null && detail.location().address() != null) {
                houseToUpdate.setAddress(detail.location().address().completeAdress());
            }

            if (detail.files() != null && detail.files().locationImage() != null && !detail.files().locationImage().isEmpty()) {
                houseToUpdate.setImageUrl(detail.files().locationImage().get(0).address());
            }

            houseRepository.save(houseToUpdate);
            log.info("Successfully updated details for house ID: {}", houseToUpdate.getId());
        } catch (Exception e) {
            log.error("Error fetching details for house ID: {}", houseToUpdate.getId(), e);
        }
    }
}
