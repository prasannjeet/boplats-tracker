package com.prasannjeet.vaxjobostader.service.geocoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * OpenStreetMap Nominatim geocoder. Free, no API key — only a User-Agent
 * header is required by the Nominatim usage policy. The policy also caps
 * use at one request per second, so this class serialises calls and
 * inserts a sleep when needed.
 */
@Component
@Slf4j
public class NominatimGeocodingProvider implements GeocodingProvider {

    private static final String PROVIDER_NAME = "nominatim";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final AppConfig appConfig;
    private final Object rateGate = new Object();
    private long lastCallEpochMs = 0L;

    public NominatimGeocodingProvider(ObjectMapper objectMapper, AppConfig appConfig) {
        this.objectMapper = objectMapper;
        this.appConfig = appConfig;
        this.httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    }

    @Override
    public String name() {
        return PROVIDER_NAME;
    }

    @Override
    public GeocodeOutcome geocode(String address) {
        if (address == null || address.isBlank()) return GeocodeOutcome.notFound();

        respectRateLimit();

        String url = appConfig.getGeocoding().getBaseUrl()
            + "/search?format=json&limit=1&q="
            + URLEncoder.encode(address, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .header("User-Agent", appConfig.getGeocoding().getUserAgent())
            .header("Accept", "application/json")
            .timeout(Duration.ofSeconds(15))
            .GET()
            .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Nominatim returned HTTP {} for address '{}'", response.statusCode(), address);
                return GeocodeOutcome.failed();
            }

            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray() || root.isEmpty()) {
                return GeocodeOutcome.notFound();
            }

            JsonNode first = root.get(0);
            JsonNode lat = first.get("lat");
            JsonNode lon = first.get("lon");
            if (lat == null || lon == null) {
                log.warn("Nominatim response missing lat/lon for '{}'", address);
                return GeocodeOutcome.notFound();
            }
            return GeocodeOutcome.found(Double.parseDouble(lat.asText()), Double.parseDouble(lon.asText()));

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return GeocodeOutcome.failed();
        } catch (Exception e) {
            log.warn("Nominatim geocode failed for '{}': {}", address, e.getMessage());
            return GeocodeOutcome.failed();
        }
    }

    /**
     * Nominatim usage policy: at most 1 request/sec. We serialise calls and
     * sleep just long enough to honour the configured gap. With a single
     * sync thread this is sufficient; if we ever parallelise geocoding we
     * will need a token bucket instead.
     */
    private void respectRateLimit() {
        long gap = appConfig.getGeocoding().getRateLimitMs();
        synchronized (rateGate) {
            long now = System.currentTimeMillis();
            long sinceLast = now - lastCallEpochMs;
            if (sinceLast < gap) {
                try {
                    Thread.sleep(gap - sinceLast);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            lastCallEpochMs = System.currentTimeMillis();
        }
    }
}
