package com.prasannjeet.vaxjobostader.service.geocoding;

import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * One-shot backfill: on startup, find every active row that has an address
 * but no coordinates, and geocode it. Subsequent runs are no-ops because
 * each successful geocoding writes back the coordinates and
 * `findInternalIdsNeedingGeocode` returns an empty list.
 *
 * Runs in a background thread so application startup is never blocked by
 * the geocoding rate limit. The detail sync handles new listings going
 * forward, so this exists purely to catch rows that existed before
 * geocoding was wired in.
 */
@Service
@Slf4j
public class HouseGeocodeBackfillService implements ApplicationRunner {

    private final HouseRepository houseRepository;
    private final AddressGeocodeService addressGeocodeService;
    private final AppConfig appConfig;

    // Self-reference through the Spring proxy so @Transactional applies
    // when backfillAll() loops back into the same bean. @Lazy breaks the
    // bean-creation cycle that would otherwise occur.
    @Autowired
    @Lazy
    private HouseGeocodeBackfillService self;

    public HouseGeocodeBackfillService(
        HouseRepository houseRepository,
        AddressGeocodeService addressGeocodeService,
        AppConfig appConfig
    ) {
        this.houseRepository = houseRepository;
        this.addressGeocodeService = addressGeocodeService;
        this.appConfig = appConfig;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!appConfig.getGeocoding().isEnabled()) {
            log.info("Geocoding disabled; skipping backfill.");
            return;
        }
        Thread t = new Thread(this::backfillAll, "house-geocode-backfill");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Package-private so tests can drive it synchronously if needed.
     * Self-injected `self.geocodeOne` so the per-house @Transactional
     * applies even though the loop is in the same bean.
     */
    void backfillAll() {
        try {
            List<Long> ids = houseRepository.findInternalIdsNeedingGeocode();
            if (ids.isEmpty()) {
                log.info("House geocode backfill: nothing to do.");
                return;
            }
            log.info("House geocode backfill: starting for {} row(s).", ids.size());
            int resolved = 0;
            int unresolved = 0;
            for (Long id : ids) {
                try {
                    if (self.geocodeOne(id)) resolved++;
                    else unresolved++;
                } catch (Exception e) {
                    unresolved++;
                    log.warn("Backfill geocode failed for internalId={}: {}", id, e.getMessage());
                }
            }
            log.info("House geocode backfill complete. Resolved: {}, unresolved: {}.", resolved, unresolved);
        } catch (Exception e) {
            log.error("House geocode backfill aborted", e);
        }
    }

    @Transactional
    public boolean geocodeOne(Long internalId) {
        House house = houseRepository.findById(internalId).orElse(null);
        if (house == null) return false;
        if (house.getCompleteAddress() == null || house.getCompleteAddress().isBlank()) return false;
        // Idempotent: another path may have filled it in between query and now.
        if (house.getLatitude() != null && house.getLongitude() != null
                && house.getCompleteAddress().equals(house.getGeocodedAddress())) {
            return true;
        }

        return addressGeocodeService.resolve(house.getCompleteAddress())
            .map(coords -> {
                house.setLatitude(coords.latitude());
                house.setLongitude(coords.longitude());
                house.setGeocodedAddress(house.getCompleteAddress());
                house.setGeocodedAt(Instant.now());
                houseRepository.save(house);
                return true;
            })
            .orElse(false);
    }
}
