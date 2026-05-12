package com.prasannjeet.vaxjobostader.service.geocoding;

import java.util.Optional;

/**
 * Result of a geocoding attempt.
 *
 * Three states:
 *  - FOUND     — coordinates are present.
 *  - NOT_FOUND — provider returned no match; cache permanently to avoid
 *                retrying an unresolvable string.
 *  - FAILED    — transient failure (network, HTTP 5xx, parse error). NOT
 *                cached, so the next sync will retry.
 */
public record GeocodeOutcome(Status status, Coordinates coordinates) {

    public enum Status { FOUND, NOT_FOUND, FAILED }

    public static GeocodeOutcome found(double lat, double lng) {
        return new GeocodeOutcome(Status.FOUND, new Coordinates(lat, lng));
    }

    public static GeocodeOutcome notFound() {
        return new GeocodeOutcome(Status.NOT_FOUND, null);
    }

    public static GeocodeOutcome failed() {
        return new GeocodeOutcome(Status.FAILED, null);
    }

    public Optional<Coordinates> coordinatesOptional() {
        return Optional.ofNullable(coordinates);
    }
}
