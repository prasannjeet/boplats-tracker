package com.prasannjeet.vaxjobostader.service.geocoding;

import com.prasannjeet.vaxjobostader.jpa.AddressGeocode;
import com.prasannjeet.vaxjobostader.jpa.AddressGeocodeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Cache-first geocoding lookup. Houses share addresses across re-listings,
 * so a single building is geocoded at most once. Persisted outcomes:
 *   SUCCESS   → coordinates stored; subsequent calls return them with no
 *               provider round-trip.
 *   NOT_FOUND → stored too, so an unresolvable string isn't retried each
 *               cycle.
 *   FAILED    → NOT stored; the next caller will retry, which is what we
 *               want for transient errors.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AddressGeocodeService {

    private final AddressGeocodeRepository repository;
    private final GeocodingProvider provider;

    @Transactional
    public Optional<Coordinates> resolve(String address) {
        if (address == null) return Optional.empty();
        String key = address.trim();
        if (key.isEmpty()) return Optional.empty();

        Optional<AddressGeocode> cached = repository.findById(key);
        if (cached.isPresent()) {
            AddressGeocode hit = cached.get();
            return hit.getStatus() == AddressGeocode.Status.SUCCESS
                ? Optional.of(new Coordinates(hit.getLatitude(), hit.getLongitude()))
                : Optional.empty();
        }

        GeocodeOutcome outcome = provider.geocode(key);
        switch (outcome.status()) {
            case FOUND -> {
                AddressGeocode row = new AddressGeocode(
                    key,
                    outcome.coordinates().latitude(),
                    outcome.coordinates().longitude(),
                    AddressGeocode.Status.SUCCESS,
                    provider.name(),
                    Instant.now()
                );
                repository.save(row);
                log.info("Geocoded address '{}' → ({}, {})", key,
                    outcome.coordinates().latitude(), outcome.coordinates().longitude());
                return Optional.of(outcome.coordinates());
            }
            case NOT_FOUND -> {
                AddressGeocode row = new AddressGeocode(
                    key, null, null,
                    AddressGeocode.Status.NOT_FOUND,
                    provider.name(),
                    Instant.now()
                );
                repository.save(row);
                log.info("Geocoded address '{}' → not found (cached)", key);
                return Optional.empty();
            }
            case FAILED -> {
                // Intentionally not cached: next sync should retry.
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
