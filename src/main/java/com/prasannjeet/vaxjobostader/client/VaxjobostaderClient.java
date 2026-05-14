package com.prasannjeet.vaxjobostader.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseDetail;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListResponse;
import com.prasannjeet.vaxjobostader.client.dto.house.ObjectTypeMetadata;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.util.StaticUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class VaxjobostaderClient {

    private static final int PAGE_SIZE = 25;
    private static final long PAGE_FETCH_DELAY_MS = 200;

    private final ObjectMapper mapper;
    private final URI hostUri;
    private final String apiKey;
    private final HttpClient httpClient;
    private final long pageFetchDelayMs;

    @Autowired
    public VaxjobostaderClient(AppConfig config) {
        this.mapper = StaticUtils.getMapper();
        this.hostUri = URI.create(config.getVbUrl());
        this.apiKey = config.getVbApiKey();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.pageFetchDelayMs = PAGE_FETCH_DELAY_MS;
        log.info("VaxjobostaderClient initialized with URI: {}", this.hostUri);
    }

    /** Package-private constructor for unit tests — allows injecting a mock HttpClient with zero inter-page delay. */
    VaxjobostaderClient(URI hostUri, String apiKey, HttpClient httpClient, ObjectMapper mapper) {
        this.hostUri = hostUri;
        this.apiKey = apiKey;
        this.httpClient = httpClient;
        this.mapper = mapper;
        this.pageFetchDelayMs = 0;
    }

    private HttpRequest.Builder getBaseRequestBuilder(URI uri) {
        return HttpRequest.newBuilder()
                .uri(uri)
                .header("X-Api-Key", this.apiKey)
                .header("Accept", "application/json, text/plain, */*");
    }

    /** Fetches ALL property types (no type filter) via paginated limit/offset. */
    public List<HouseListItem> getAllPropertiesList() throws IOException, InterruptedException {
        List<HouseListItem> all = new ArrayList<>();
        int offset = 0;
        int total = Integer.MAX_VALUE;

        while (offset < total) {
            URI pageUri = URI.create(hostUri + "?limit=" + PAGE_SIZE + "&offset=" + offset);
            HttpRequest request = getBaseRequestBuilder(pageUri).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                log.error("Failed to fetch list page at offset={}. Status: {}", offset, response.statusCode());
                throw new IOException("Failed to fetch list from Momentum API at offset=" + offset);
            }
            HouseListResponse page = mapper.readValue(response.body(), HouseListResponse.class);
            List<HouseListItem> items = page.items() != null ? page.items() : List.of();
            all.addAll(items);
            if (total == Integer.MAX_VALUE) {
                if (page.count() != null) {
                    total = page.count();
                } else {
                    log.warn("API returned null count at offset={}; treating current page as final page", offset);
                    total = offset + items.size();
                }
            }
            offset += PAGE_SIZE;
            if (!items.isEmpty() && offset < total) {
                Thread.sleep(pageFetchDelayMs);
            }
        }
        log.info("Fetched {} items from list API.", all.size());
        return all;
    }

    /** Returns null when the API returns a non-200 status (e.g. unknown type ID). */
    public ObjectTypeMetadata getTypeMetadata(String typeId) throws IOException, InterruptedException {
        URI metaUri = URI.create(hostUri + "/types/" + typeId);
        HttpRequest request = getBaseRequestBuilder(metaUri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.warn("Type metadata not found for typeId={}. Status: {}", typeId, response.statusCode());
            return null;
        }
        return mapper.readValue(response.body(), ObjectTypeMetadata.class);
    }

    public HouseDetail getPropertyDetail(String id) throws IOException, InterruptedException {
        URI detailUri = URI.create(hostUri + "/" + id);
        HttpRequest request = getBaseRequestBuilder(detailUri).GET().build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            log.error("Failed to fetch detail for ID: {}. Status: {}", id, response.statusCode());
            throw new IOException("Failed to fetch detail from Momentum API");
        }
        return mapper.readValue(response.body(), HouseDetail.class);
    }
}
