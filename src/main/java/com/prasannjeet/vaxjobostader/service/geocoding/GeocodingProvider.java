package com.prasannjeet.vaxjobostader.service.geocoding;

/**
 * Geocodes a single free-form address string. Implementations are
 * expected to enforce any provider-imposed rate limits internally.
 */
public interface GeocodingProvider {

    /**
     * Stable identifier persisted alongside cached results, so cache rows
     * remember which provider produced them.
     */
    String name();

    GeocodeOutcome geocode(String address);
}
