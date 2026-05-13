package com.prasannjeet.vaxjobostader.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseDetail;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseFiles;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListResponse;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseLocation;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseFloorplan;
import com.prasannjeet.vaxjobostader.jpa.HouseFloorplanRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseImage;
import com.prasannjeet.vaxjobostader.jpa.HouseImageRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import com.prasannjeet.vaxjobostader.service.geocoding.AddressGeocodeService;
import com.prasannjeet.vaxjobostader.service.geocoding.Coordinates;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class HouseSyncService {

    private final VaxjobostaderClient vaxjobostaderClient;
    private final HouseRepository houseRepository;
    private final HouseImageRepository houseImageRepository;
    private final HouseFloorplanRepository houseFloorplanRepository;
    private final AppConfig appConfig;
    private final TaskScheduler taskScheduler;
    private final ObjectMapper objectMapper;
    private final AddressGeocodeService addressGeocodeService;

    // Self-reference through the Spring proxy so @Transactional applies when
    // the scheduled tick calls back into the service. A plain this.method()
    // would bypass the proxy and run without a transaction, which breaks
    // the @Modifying delete-by-external-id queries.
    @Autowired
    @Lazy
    private HouseSyncService self;

    private final AtomicReference<ScheduledFuture<?>> nextDetailRun = new AtomicReference<>();

    /**
     * Reconciles the API listing with the database. Order matters:
     * 1) reconcile API rows by the DB identity (id + available_from), even if
     *    the matching row is currently ended
     * 2) end active rows missing from the API response
     * 3) end any house whose application deadline has passed
     *
     * Doing the deadline sweep AFTER the reconcile means a listing that is past
     * its deadline but still present in the API response is matched (and updated)
     * before being ended, instead of producing a duplicate row.
     */
    @Scheduled(fixedDelayString = "${appconfig.listApiCacheDurationHours:24}", timeUnit = TimeUnit.HOURS)
    @Transactional
    public void syncHouseList() {
        log.info("Starting house list synchronization...");
        Date now = new Date();
        LocalDate today = LocalDate.now();
        try {
            HouseListResponse response = vaxjobostaderClient.getPropertiesList();

            int inserted = 0;
            int updated = 0;
            int skipped = 0;
            int endedMissing = 0;

            // Reconcile only when the API response is healthy. Otherwise we'd
            // mark every active house ended just because the API hiccupped.
            if (response != null && response.items() != null && !response.items().isEmpty()) {
                List<HouseListItem> apiItems = response.items();
                List<House> active = houseRepository.findAllByEndDateIsNull();
                Map<HouseKey, House> activeByKey = new HashMap<>(active.size());
                for (House h : active) {
                    activeByKey.put(HouseKey.of(h.getId(), h.getAvailableFrom()), h);
                }

                Set<String> apiExternalIds = new HashSet<>();
                for (HouseListItem item : apiItems) {
                    if (item.id() != null) {
                        apiExternalIds.add(item.id());
                    }
                }

                Map<HouseKey, House> existingByKey = new HashMap<>();
                if (!apiExternalIds.isEmpty()) {
                    for (House h : houseRepository.findAllByExternalIds(apiExternalIds)) {
                        existingByKey.put(HouseKey.of(h.getId(), h.getAvailableFrom()), h);
                    }
                }

                List<House> toSave = new ArrayList<>(apiItems.size());

                for (HouseListItem item : apiItems) {
                    Date availableFrom = item.availability() == null ? null : item.availability().availableFrom();
                    if (availableFrom == null) {
                        log.warn("API item id={} has no availableFrom; skipping.", item.id());
                        skipped++;
                        continue;
                    }

                    HouseKey key = HouseKey.of(item.id(), availableFrom);
                    House existing = existingByKey.get(key);
                    activeByKey.remove(key);
                    House house = existing != null ? existing : new House();
                    boolean isNew = existing == null;

                    house.setId(item.id());
                    house.setLocalId(item.localId());
                    house.setDescription(item.description());
                    house.setAvailableFrom(availableFrom);
                    house.setEndDate(null);
                    house.setType(item.type());
                    house.setDisplayName(item.displayName());

                    if (item.location() != null && item.location().area() != null) {
                        house.setAreaName(item.location().area().displayName());
                    }
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

                    toSave.add(house);
                    if (isNew) inserted++;
                    else updated++;
                }

                houseRepository.saveAll(toSave);

                // Whatever remained in activeByKey is no longer in the API response → ended.
                List<House> toEnd = new ArrayList<>(activeByKey.values());
                for (House h : toEnd) {
                    h.setEndDate(now);
                }
                if (!toEnd.isEmpty()) {
                    houseRepository.saveAll(toEnd);
                }
                endedMissing = toEnd.size();
            } else {
                log.warn("Received empty or null list response from API. Skipping reconcile but still running deadline sweep.");
            }

            // Always run the deadline sweep — it's independent of API health and
            // a hiccup shouldn't leave past-deadline rows lingering as active.
            int pastDeadline = houseRepository.markPastDeadlineEnded(today, now);

            log.info("House list sync complete. Inserted: {}, updated: {}, ended-missing: {}, ended-past-deadline: {}, skipped: {}",
                inserted, updated, endedMissing, pastDeadline, skipped);

        } catch (Exception e) {
            log.error("Error during house list synchronization", e);
        }
    }

    @PostConstruct
    void scheduleFirstDetailRun() {
        scheduleNextDetailRun(Duration.ofSeconds(appConfig.getDetailFetchMinDelaySeconds()));
    }

    @PreDestroy
    void cancelDetailRun() {
        ScheduledFuture<?> f = nextDetailRun.getAndSet(null);
        if (f != null) f.cancel(false);
    }

    /**
     * Picks the single oldest stale active house, refreshes its detail, and
     * re-schedules itself with a delay derived from active-house count and
     * the refresh window. Cadence is therefore a consequence of workload.
     */
    void runDetailTick() {
        try {
            self.syncOneHouseDetail();
        } catch (Exception e) {
            log.error("Unexpected error in detail tick", e);
        } finally {
            scheduleNextDetailRun(computeNextDelay());
        }
    }

    @Transactional
    public void syncOneHouseDetail() {
        Instant cutoff = Instant.now().minus(appConfig.getDetailRefreshIntervalHours(), ChronoUnit.HOURS);
        List<House> batch = houseRepository.findHousesNeedingDetailRefresh(
            cutoff, PageRequest.of(0, 1)
        );
        if (batch.isEmpty()) {
            log.debug("No houses needing detail refresh.");
            return;
        }

        House house = batch.get(0);
        Instant fetchedAt = Instant.now();
        try {
            HouseDetail detail = vaxjobostaderClient.getPropertyDetail(house.getId());
            if (detail == null) {
                log.warn("Null detail response for house {}.", house.getId());
            } else {
                applyDetailToHouse(house, detail);
            }
        } catch (Exception e) {
            log.error("Failed to fetch detail for house {}.", house.getId(), e);
        } finally {
            // Always advance the timestamp so a single problematic house cannot
            // block the queue by being picked first repeatedly.
            house.setLastDetailFetchedAt(fetchedAt);
            houseRepository.save(house);
        }
    }

    Duration computeNextDelay() {
        long active = houseRepository.countByEndDateIsNull();
        int min = appConfig.getDetailFetchMinDelaySeconds();
        int max = appConfig.getDetailFetchMaxDelaySeconds();
        int idle = appConfig.getDetailFetchIdleDelaySeconds();

        if (active <= 0) {
            return Duration.ofSeconds(idle);
        }

        long windowSeconds = appConfig.getDetailRefreshIntervalHours() * 3600L;
        long perHouse = Math.max(1L, windowSeconds / active);
        long clamped = Math.min(max, Math.max(min, perHouse));
        return Duration.ofSeconds(clamped);
    }

    private void scheduleNextDetailRun(Duration delay) {
        ScheduledFuture<?> scheduled = taskScheduler.schedule(
            this::runDetailTick,
            Instant.now().plus(delay)
        );
        ScheduledFuture<?> previous = nextDetailRun.getAndSet(scheduled);
        if (previous != null) previous.cancel(false);
    }

    private void applyDetailToHouse(House house, HouseDetail detail) {
        if (detail.type() != null) house.setType(detail.type());
        if (detail.displayName() != null) house.setDisplayName(detail.displayName());
        if (detail.number() != null) house.setNumber(detail.number());

        HouseLocation location = detail.location();
        if (location != null) {
            if (location.area() != null) {
                house.setAreaName(location.area().displayName());
            }
            house.setFloorDisplayName(location.floorDisplayName());
            HouseLocation.HouseAddress addr = location.address();
            if (addr != null) {
                house.setStreet(addr.street());
                house.setStreetNumber(addr.streetnumber());
                house.setPostcode(addr.postcode());
                house.setCity(addr.city());
                house.setCountry(addr.country());
                house.setCompleteAddress(addr.completeAdress());
                if (addr.completeAdress() != null && !addr.completeAdress().isBlank()) {
                    house.setAddress(addr.completeAdress());
                }
            }
            house.setAreaPathJson(toJsonOrNull(location.areaPath()));
        }
        if (detail.application() != null) {
            house.setApplicationDeadline(detail.application().openTo());
        }
        if (detail.files() != null) {
            replaceImages(house.getId(), detail.files().locationImage());
            replaceFloorplans(house.getId(), detail.files().floorplan());
            if (detail.files().locationImage() != null
                    && !detail.files().locationImage().isEmpty()) {
                house.setImageUrl(detail.files().locationImage().get(0).address());
            }
        }
        // Sticky: only overwrite when the API actually returns a value.
        // null means "no cut-off computed yet for this listing", not "reset to nothing".
        Double incoming = detail.queuePointsCurrentPositionX();
        if (incoming != null) {
            house.setQueuePoints(incoming);
        }

        geocodeIfChanged(house);
    }

    /**
     * Resolves coordinates for the house's current address, but only when
     * the address is new or has changed. Re-listings of the same building
     * therefore make at most one provider call total (shared via the
     * AddressGeocodeService cache), and unchanged addresses make zero.
     */
    private void geocodeIfChanged(House house) {
        if (!appConfig.getGeocoding().isEnabled()) return;

        String current = house.getCompleteAddress();
        if (current == null || current.isBlank()) return;

        boolean alreadyResolved = current.equals(house.getGeocodedAddress())
            && house.getLatitude() != null
            && house.getLongitude() != null;
        if (alreadyResolved) return;

        addressGeocodeService.resolve(current).ifPresent(coords -> {
            house.setLatitude(coords.latitude());
            house.setLongitude(coords.longitude());
            house.setGeocodedAddress(current);
            house.setGeocodedAt(Instant.now());
        });
    }

    private void replaceImages(String houseExternalId, List<HouseFiles.HouseImage> apiImages) {
        if (apiImages == null) return;
        houseImageRepository.deleteByHouseExternalId(houseExternalId);
        if (apiImages.isEmpty()) return;
        List<HouseImage> rows = new ArrayList<>(apiImages.size());
        int order = 0;
        for (HouseFiles.HouseImage img : apiImages) {
            HouseImage row = new HouseImage();
            row.setHouseExternalId(houseExternalId);
            row.setDisplayName(img.displayName());
            row.setMimeType(img.mimeType());
            row.setAddress(img.address());
            row.setLinkedToType(img.linkedToType());
            row.setSortOrder(order++);
            rows.add(row);
        }
        houseImageRepository.saveAll(rows);
    }

    private void replaceFloorplans(String houseExternalId, List<HouseFiles.HouseFloorplan> apiFloorplans) {
        if (apiFloorplans == null) return;
        houseFloorplanRepository.deleteByHouseExternalId(houseExternalId);
        if (apiFloorplans.isEmpty()) return;
        List<HouseFloorplan> rows = new ArrayList<>(apiFloorplans.size());
        int order = 0;
        for (HouseFiles.HouseFloorplan fp : apiFloorplans) {
            HouseFloorplan row = new HouseFloorplan();
            row.setHouseExternalId(houseExternalId);
            row.setDisplayName(fp.displayName());
            row.setMimeType(fp.mimeType());
            row.setAddress(fp.address());
            row.setSortOrder(order++);
            rows.add(row);
        }
        houseFloorplanRepository.saveAll(rows);
    }

    private String toJsonOrNull(Object value) {
        if (value == null) return null;
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize {} to JSON: {}", value.getClass().getSimpleName(), e.getMessage());
            return null;
        }
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

    /**
     * Date-truncated key. The DB column is DATE, so loaded values have time=00:00,
     * but API values can carry a time component. Comparing via LocalDate avoids
     * spurious "new instance" matches when the same calendar day has different
     * times on the two sides.
     */
    private record HouseKey(String id, LocalDate availableFrom) {
        static HouseKey of(String id, Date date) {
            LocalDate ld;
            if (date == null) {
                ld = null;
            } else if (date instanceof java.sql.Date sqlDate) {
                // java.sql.Date.toInstant() throws UnsupportedOperationException by spec.
                ld = sqlDate.toLocalDate();
            } else {
                ld = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
            return new HouseKey(id, ld);
        }
    }
}
