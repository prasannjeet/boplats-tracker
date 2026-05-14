# Expand API Fetch to All Property Types — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove the `type=residential` filter and fetch all 1,297 objects from the Momentum API via paginated limit/offset, capturing the new queue type, rental object type, and amenities data in the database alongside a new `object_type` metadata table.

**Architecture:** The API client gains a paginated `getAllPropertiesList()` that returns a flat `List<HouseListItem>` (hides pagination from callers). The sync service is updated to set the four new `house` columns and call `syncTypeMetadata()` after every list sync. Two Liquibase changesets handle schema evolution; no backfill is needed since the first sync after deployment populates everything.

**Tech Stack:** Java 25, Spring Boot 4, Spring Data JPA, Liquibase, Jackson, Mockito/JUnit 5, H2 (tests), MariaDB/MySQL (production)

---

## File Map

| Action | File |
|---|---|
| Create | `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseThumbnail.java` |
| Create | `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseIncluded.java` |
| Create | `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/ObjectTypeMetadata.java` |
| Modify | `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseListItem.java` |
| Modify | `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDetail.java` |
| Modify | `src/main/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClient.java` |
| Create | `src/main/resources/db/changelog/house/house-type-columns.xml` |
| Create | `src/main/resources/db/changelog/house/object-type-table.xml` |
| Modify | `src/main/resources/db.changelog-master.xml` |
| Modify | `src/main/java/com/prasannjeet/vaxjobostader/jpa/House.java` |
| Create | `src/main/java/com/prasannjeet/vaxjobostader/jpa/ObjectType.java` |
| Create | `src/main/java/com/prasannjeet/vaxjobostader/jpa/ObjectTypeRepository.java` |
| Modify | `src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java` |
| Modify | `src/test/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDtoTest.java` |
| Create | `src/test/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClientPaginationTest.java` |
| Modify | `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java` |

---

## Task 1: Create new support DTO records

**Files:**
- Create: `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseThumbnail.java`
- Create: `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseIncluded.java`
- Create: `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/ObjectTypeMetadata.java`

- [ ] **Step 1: Create HouseThumbnail**

```java
// src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseThumbnail.java
package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseThumbnail(boolean exists, String displayName, String version) {}
```

- [ ] **Step 2: Create HouseIncluded**

```java
// src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseIncluded.java
package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseIncluded(String displayName) {}
```

- [ ] **Step 3: Create ObjectTypeMetadata**

```java
// src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/ObjectTypeMetadata.java
package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ObjectTypeMetadata(
    String displayName,
    String description,
    Integer maxNumberOfApplications,
    Integer minNumberOfRooms,
    Integer maxNumberOfRooms,
    Double minSize,
    Double maxSize,
    Double minPrice,
    Double maxPrice,
    Integer numberOfMarketObjects
) {}
```

- [ ] **Step 4: Verify compile**

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/amazon-corretto-25.jdk/Contents/Home
mymvn compile -pl . --no-transfer-progress 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseThumbnail.java \
        src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseIncluded.java \
        src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/ObjectTypeMetadata.java
git commit -m "feat: add HouseThumbnail, HouseIncluded, ObjectTypeMetadata DTO records"
```

---

## Task 2: Extend HouseListItem — add queueType, rentalObjectType, thumbnail

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseListItem.java`
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDtoTest.java`
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java`

The existing `list-response.json` fixture already contains `queueType` and `thumbnail` fields. The test assertions just need to be added; adding the fields to the record makes them deserialize automatically.

- [ ] **Step 1: Write the failing test assertions**

In `HouseDtoTest.testListResponseDeserialization()`, add after the existing assertions:

```java
assertThat(firstItem.queueType()).isEqualTo("residential");
assertThat(firstItem.thumbnail()).isNotNull();
assertThat(firstItem.thumbnail().exists()).isTrue();
assertThat(firstItem.thumbnail().version()).isEqualTo("f-565812");
```

Run the test — it fails to compile because `queueType()` and `thumbnail()` don't exist yet.

```bash
mymvn test -Dtest=HouseDtoTest --no-transfer-progress 2>&1 | tail -10
```

Expected: compilation error `cannot find symbol: method queueType()`

- [ ] **Step 2: Update HouseListItem to add the three new fields**

Replace the entire file with:

```java
package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseListItem(
    String id,
    String localId,
    String displayName,
    String description,
    String type,
    String queueType,
    String rentalObjectType,
    HousePricing pricing,
    HouseLocation location,
    HouseAvailability availability,
    HouseSize size,
    HouseThumbnail thumbnail
) {}
```

- [ ] **Step 3: Fix the listItem() helper in HouseSyncServiceTest**

`HouseListItem` is constructed positionally in the test helper — adding three fields shifts the constructor. Replace both `listItem()` overloads with:

```java
private static HouseListItem listItem(String id, Date availableFrom) {
    return listItem(id, availableFrom, 0.0);
}

private static HouseListItem listItem(String id, Date availableFrom, double price) {
    return new HouseListItem(
        id,
        "local-" + id,
        "display",
        "desc",
        "residential",
        "residential",
        null,
        new HousePricing(price),
        null,
        availableFrom == null ? null : new HouseAvailability(availableFrom),
        new HouseSize("1 rok", "1", 30.0),
        null
    );
}
```

- [ ] **Step 4: Run the tests**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -15
```

Expected: `BUILD SUCCESS` — `HouseDtoTest` now passes the new assertions.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseListItem.java \
        src/test/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDtoTest.java \
        src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java
git commit -m "feat: add queueType, rentalObjectType, thumbnail to HouseListItem"
```

---

## Task 3: Extend HouseDetail — add queueType, queueTypeDisplayName, rentalObjectType, nrApplications, included

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDetail.java`
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDtoTest.java`
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java`

The existing `detail-response.json` fixture already has `queueType`, `queueTypeDisplayName`, `nrApplications`, and `included`.

- [ ] **Step 1: Write the failing test assertions**

In `HouseDtoTest.testDetailResponseDeserialization()`, add after the existing assertions:

```java
assertThat(detail.queueType()).isEqualTo("residential");
assertThat(detail.queueTypeDisplayName()).isEqualTo("Bostad");
assertThat(detail.nrApplications()).isEqualTo(0);
assertThat(detail.included()).hasSize(5);
assertThat(detail.included().get(0).displayName()).isEqualTo("Bredband finns");
```

Run the test — it fails to compile.

```bash
mymvn test -Dtest=HouseDtoTest --no-transfer-progress 2>&1 | tail -10
```

Expected: compilation error `cannot find symbol: method queueType()`

- [ ] **Step 2: Update HouseDetail to add five new fields**

Replace the entire file with:

```java
package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseDetail(
    String id,
    String localId,
    String number,
    String displayName,
    String type,
    String queueType,
    String queueTypeDisplayName,
    String rentalObjectType,
    Integer nrApplications,
    HousePricing pricing,
    HouseLocation location,
    HouseAvailability availability,
    HouseApplication application,
    HouseSize size,
    HouseFiles files,
    Double queuePointsCurrentPositionX,
    List<HouseIncluded> included
) {}
```

- [ ] **Step 3: Fix the detailWithQueuePoints() helper in HouseSyncServiceTest**

`HouseDetail` now has 17 constructor parameters. Update the helper:

```java
private static HouseDetail detailWithQueuePoints(Double qp) {
    return new HouseDetail(
        null, null, null, null, null,
        null, null, null, null,
        null, null, null, null, null, null,
        qp,
        null
    );
}
```

- [ ] **Step 4: Run the tests**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -15
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDetail.java \
        src/test/java/com/prasannjeet/vaxjobostader/client/dto/house/HouseDtoTest.java \
        src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java
git commit -m "feat: add queueType, nrApplications, included and related fields to HouseDetail"
```

---

## Task 4: Refactor VaxjobostaderClient — paginated list fetch + type metadata

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClient.java`
- Create: `src/test/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClientPaginationTest.java`
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java`

- [ ] **Step 1: Write VaxjobostaderClientPaginationTest (failing)**

Create the test file. It uses the package-private constructor (added in step 2) that accepts an injected `HttpClient` and zero inter-page delay:

```java
package com.prasannjeet.vaxjobostader.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.ObjectTypeMetadata;
import com.prasannjeet.vaxjobostader.util.StaticUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VaxjobostaderClientPaginationTest {

    @Mock HttpClient httpClient;
    @Mock HttpResponse<String> httpResponse;

    private VaxjobostaderClient client;
    private final ObjectMapper mapper = StaticUtils.getMapper();

    @BeforeEach
    void setUp() {
        client = new VaxjobostaderClient(
            URI.create("https://example.com/api"),
            "test-key",
            httpClient,
            mapper
        );
    }

    @Test
    void getAllPropertiesList_singlePage_returnsAllItems() throws Exception {
        String body = """
            {"count":2,"items":[
                {"id":"a","localId":"1","displayName":"A","type":"residential","queueType":"residential","availability":{"availableFrom":"/Date(1785535200000)/"},"thumbnail":{"exists":false}},
                {"id":"b","localId":"2","displayName":"B","type":"residential","queueType":"residential","availability":{"availableFrom":"/Date(1785535200000)/"},"thumbnail":{"exists":false}}
            ]}""";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        List<HouseListItem> result = client.getAllPropertiesList();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).id()).isEqualTo("a");
        verify(httpClient, times(1)).send(any(), any());
    }

    @Test
    void getAllPropertiesList_multiPage_fetchesAllPagesAndCombines() throws Exception {
        String page1 = buildPage(50, 25, 0);
        String page2 = buildPage(50, 25, 25);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(page1, page2);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        List<HouseListItem> result = client.getAllPropertiesList();

        assertThat(result).hasSize(50);
        verify(httpClient, times(2)).send(any(), any());
    }

    @Test
    void getAllPropertiesList_nonOkStatus_throwsIOException() throws Exception {
        when(httpResponse.statusCode()).thenReturn(500);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        assertThatThrownBy(() -> client.getAllPropertiesList())
            .isInstanceOf(IOException.class)
            .hasMessageContaining("offset=0");
    }

    @Test
    void getTypeMetadata_nonOkStatus_returnsNull() throws Exception {
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        assertThat(client.getTypeMetadata("unknownType")).isNull();
    }

    @Test
    void getTypeMetadata_okStatus_deserializesMetadata() throws Exception {
        String body = "{\"displayName\":\"Bostad\",\"description\":\"Standard apartments\",\"numberOfMarketObjects\":169}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any())).thenReturn(httpResponse);

        ObjectTypeMetadata meta = client.getTypeMetadata("residential");

        assertThat(meta).isNotNull();
        assertThat(meta.displayName()).isEqualTo("Bostad");
        assertThat(meta.numberOfMarketObjects()).isEqualTo(169);
    }

    /** Builds a JSON list page where each item has a unique id = "item-{start+i}". */
    private static String buildPage(int total, int pageSize, int start) {
        StringBuilder sb = new StringBuilder("{\"count\":" + total + ",\"items\":[");
        for (int i = 0; i < pageSize; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"id\":\"item-").append(start + i)
              .append("\",\"localId\":\"").append(start + i)
              .append("\",\"displayName\":\"Item ").append(start + i)
              .append("\",\"type\":\"residential\",\"queueType\":\"residential\"")
              .append(",\"availability\":{\"availableFrom\":\"/Date(1785535200000)/\"}")
              .append(",\"thumbnail\":{\"exists\":false}}");
        }
        sb.append("]}");
        return sb.toString();
    }
}
```

Run to confirm it fails (method `getAllPropertiesList` doesn't exist yet):

```bash
mymvn test -Dtest=VaxjobostaderClientPaginationTest --no-transfer-progress 2>&1 | tail -10
```

Expected: compilation error `cannot find symbol: method getAllPropertiesList()`

- [ ] **Step 2: Replace VaxjobostaderClient with the paginated implementation**

Replace the entire file:

```java
package com.prasannjeet.vaxjobostader.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseDetail;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListResponse;
import com.prasannjeet.vaxjobostader.client.dto.house.ObjectTypeMetadata;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.util.StaticUtils;
import lombok.extern.slf4j.Slf4j;
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
                total = page.count() != null ? page.count() : 0;
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
```

- [ ] **Step 3: Fix the mock calls in HouseSyncServiceTest**

`HouseSyncService` currently calls `client.getPropertiesList()` — it will be updated in Task 8, but the tests mock that method. Until Task 8, `HouseSyncService` still references the old method name which no longer exists, so the project won't compile. Fix the method name in `HouseSyncService.syncHouseList()` immediately to unblock compilation:

In `HouseSyncService.syncHouseList()`, change:
```java
HouseListResponse response = vaxjobostaderClient.getPropertiesList();
```
to:
```java
List<HouseListItem> apiItems = vaxjobostaderClient.getAllPropertiesList();
```

Also add the import at the top of `HouseSyncService`:
```java
import java.util.List;
```

And update the healthy-response guard from:
```java
if (response != null && response.items() != null && !response.items().isEmpty()) {
    List<HouseListItem> apiItems = response.items();
```
to:
```java
if (!apiItems.isEmpty()) {
```

(The rest of the method body is unchanged in this step — the new field mappings come in Task 8.)

Then update all `when(client.getPropertiesList())` stubs in `HouseSyncServiceTest`. Replace every occurrence of:
```java
when(client.getPropertiesList()).thenReturn(new HouseListResponse(..., List.of(...)))
```

with:
```java
when(client.getAllPropertiesList()).thenReturn(List.of(...))
```

Specifically, eight places need updating. Here are all the replacements:

**reconcile_insertsNewListing:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail)));
```

**reconcile_updatesExistingActiveOnMatch:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail, 250.0)));
```

**reconcile_updatesExistingEndedOnMatchAndClearsEndDate:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail, 250.0)));
```

**reconcile_endsActiveRowsMissingFromApi:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of(listItem("OTHER", avail)));
```

**reconcile_sameIdDifferentAvailableFromIsNewRow:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", newAvail)));
```

**reconcile_skipsItemsWithNullAvailableFrom:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of(listItem("BAD", null)));
```

**reconcile_invokesPastDeadlineSweep:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail)));
```

**reconcile_runsDeadlineSweepEvenWhenApiResponseEmpty:**
```java
when(client.getAllPropertiesList()).thenReturn(List.of());
```

Also remove the import for `HouseListResponse` from `HouseSyncServiceTest` if it's no longer used.

- [ ] **Step 4: Run all tests**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -20
```

Expected: `BUILD SUCCESS` — all existing reconcile tests pass, new pagination tests pass.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClient.java \
        src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java \
        src/test/java/com/prasannjeet/vaxjobostader/client/VaxjobostaderClientPaginationTest.java \
        src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java
git commit -m "feat: replace getPropertiesList with paginated getAllPropertiesList; add getTypeMetadata"
```

---

## Task 5: Liquibase migrations

**Files:**
- Create: `src/main/resources/db/changelog/house/house-type-columns.xml`
- Create: `src/main/resources/db/changelog/house/object-type-table.xml`
- Modify: `src/main/resources/db.changelog-master.xml`

- [ ] **Step 1: Create house-type-columns.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <changeSet id="house-add-type-columns" author="prasannjeet">
        <preConditions onFail="MARK_RAN">
            <not><columnExists tableName="house" columnName="queue_type"/></not>
        </preConditions>
        <addColumn tableName="house">
            <column name="queue_type" type="VARCHAR(50)"/>
            <column name="rental_object_type" type="VARCHAR(255)"/>
            <column name="nr_applications" type="INT"/>
            <column name="included_json" type="JSON"/>
        </addColumn>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 2: Create object-type-table.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd">

    <changeSet id="create-object-type-table" author="prasannjeet">
        <preConditions onFail="MARK_RAN">
            <not><tableExists tableName="object_type"/></not>
        </preConditions>
        <createTable tableName="object_type">
            <column name="type_id" type="VARCHAR(255)">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="display_name" type="VARCHAR(255)"/>
            <column name="description" type="TEXT"/>
            <column name="min_price" type="DOUBLE"/>
            <column name="max_price" type="DOUBLE"/>
            <column name="min_rooms" type="INT"/>
            <column name="max_rooms" type="INT"/>
            <column name="min_size" type="DOUBLE"/>
            <column name="max_size" type="DOUBLE"/>
            <column name="number_of_market_objects" type="INT"/>
            <column name="last_synced_at" type="DATETIME"/>
        </createTable>
    </changeSet>

</databaseChangeLog>
```

- [ ] **Step 3: Register both changesets in db.changelog-master.xml**

In `src/main/resources/db.changelog-master.xml`, append two `<include>` lines after the existing house includes:

```xml
    <include file="db/changelog/house/house-type-columns.xml" relativeToChangelogFile="true"/>
    <include file="db/changelog/house/object-type-table.xml" relativeToChangelogFile="true"/>
```

The full `db.changelog-master.xml` active section should now read:

```xml
    <!-- Active schema: the `house` table the Momentum sync writes to. -->
    <include file="db/changelog/house/create-house-table.xml" relativeToChangelogFile="true"/>
    <include file="db/changelog/house/house-images-floorplans-tables.xml" relativeToChangelogFile="true"/>
    <include file="db/changelog/house/house-geocode-columns-and-cache.xml" relativeToChangelogFile="true"/>
    <include file="db/changelog/house/house-type-columns.xml" relativeToChangelogFile="true"/>
    <include file="db/changelog/house/object-type-table.xml" relativeToChangelogFile="true"/>
```

- [ ] **Step 4: Run tests (Liquibase runs against H2 during test startup)**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -15
```

Expected: `BUILD SUCCESS` — Liquibase applies both changesets to the H2 test DB without error.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/db/changelog/house/house-type-columns.xml \
        src/main/resources/db/changelog/house/object-type-table.xml \
        src/main/resources/db.changelog-master.xml
git commit -m "feat: add Liquibase migrations for house type columns and object_type table"
```

---

## Task 6: Add new fields to House entity

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/jpa/House.java`

- [ ] **Step 1: Add four new fields to House**

After the existing `geocodedAt` field and before the `@Transient` fields, add:

```java
@Column(name = "queue_type", length = 50)
private String queueType;

@Column(name = "rental_object_type")
private String rentalObjectType;

@Column(name = "nr_applications")
private Integer nrApplications;

@JsonRawValue
@Column(name = "included_json", columnDefinition = "json")
private String includedJson;
```

- [ ] **Step 2: Run tests**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/jpa/House.java
git commit -m "feat: add queueType, rentalObjectType, nrApplications, includedJson to House entity"
```

---

## Task 7: Add ObjectType entity and ObjectTypeRepository

**Files:**
- Create: `src/main/java/com/prasannjeet/vaxjobostader/jpa/ObjectType.java`
- Create: `src/main/java/com/prasannjeet/vaxjobostader/jpa/ObjectTypeRepository.java`

- [ ] **Step 1: Create ObjectType entity**

```java
package com.prasannjeet.vaxjobostader.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "object_type")
public class ObjectType {

    @Id
    @Column(name = "type_id")
    private String typeId;

    @Column(name = "display_name")
    private String displayName;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "min_price")
    private Double minPrice;

    @Column(name = "max_price")
    private Double maxPrice;

    @Column(name = "min_rooms")
    private Integer minRooms;

    @Column(name = "max_rooms")
    private Integer maxRooms;

    @Column(name = "min_size")
    private Double minSize;

    @Column(name = "max_size")
    private Double maxSize;

    @Column(name = "number_of_market_objects")
    private Integer numberOfMarketObjects;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
}
```

- [ ] **Step 2: Create ObjectTypeRepository**

```java
package com.prasannjeet.vaxjobostader.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ObjectTypeRepository extends JpaRepository<ObjectType, String> {}
```

- [ ] **Step 3: Run tests**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/jpa/ObjectType.java \
        src/main/java/com/prasannjeet/vaxjobostader/jpa/ObjectTypeRepository.java
git commit -m "feat: add ObjectType entity and ObjectTypeRepository"
```

---

## Task 8: Update HouseSyncService — list sync, type metadata, detail mapping

**Files:**
- Modify: `src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java`
- Modify: `src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java`

- [ ] **Step 1: Write the failing tests for new sync behavior**

Add these new test methods at the end of `HouseSyncServiceTest`, before the helpers section. The tests reference `ObjectTypeRepository` (as a mock) and call `service.syncHouseList()` — they will fail to compile until the service is updated in step 2.

First, add a mock field after the existing mocks:

```java
@Mock ObjectTypeRepository objectTypeRepository;
```

Then update the manual `service` construction in `setup()`:

```java
service = new HouseSyncService(
    client, repository, imageRepository, floorplanRepository,
    appConfig, taskScheduler, new ObjectMapper(), addressGeocodeService,
    objectTypeRepository
);
```

Now add the new tests:

```java
// -------- New fields from list item --------

@Test
void listSync_setsQueueTypeFromListItem() throws Exception {
    Date avail = date(2026, 6, 1);
    when(client.getAllPropertiesList()).thenReturn(List.of(listItemWithQueueType("A", avail, "student")));
    when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
    when(repository.findAllByExternalIds(any())).thenReturn(List.of());

    service.syncHouseList();

    ArgumentCaptor<List<House>> saved = listCaptor();
    verify(repository, atLeastOnce()).saveAll(saved.capture());
    House inserted = saved.getAllValues().stream()
        .flatMap(List::stream).filter(h -> "A".equals(h.getId())).findFirst().orElseThrow();
    assertThat(inserted.getQueueType()).isEqualTo("student");
}

@Test
void listSync_setsRentalObjectTypeForParkingItem() throws Exception {
    Date avail = date(2026, 6, 1);
    when(client.getAllPropertiesList()).thenReturn(
        List.of(listItemWithRentalObjectType("P1", avail, "parking", "Parkeringsplats"))
    );
    when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
    when(repository.findAllByExternalIds(any())).thenReturn(List.of());

    service.syncHouseList();

    ArgumentCaptor<List<House>> saved = listCaptor();
    verify(repository, atLeastOnce()).saveAll(saved.capture());
    House inserted = saved.getAllValues().stream()
        .flatMap(List::stream).filter(h -> "P1".equals(h.getId())).findFirst().orElseThrow();
    assertThat(inserted.getRentalObjectType()).isEqualTo("Parkeringsplats");
}

@Test
void listSync_populatesThumbnailImageUrlWhenNotSet() throws Exception {
    Date avail = date(2026, 6, 1);
    appConfig.setVbUrl("https://example.com/api");
    when(client.getAllPropertiesList()).thenReturn(
        List.of(listItemWithThumbnail("T1", avail, "f-565812"))
    );
    when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
    when(repository.findAllByExternalIds(any())).thenReturn(List.of());

    service.syncHouseList();

    ArgumentCaptor<List<House>> saved = listCaptor();
    verify(repository, atLeastOnce()).saveAll(saved.capture());
    House inserted = saved.getAllValues().stream()
        .flatMap(List::stream).filter(h -> "T1".equals(h.getId())).findFirst().orElseThrow();
    assertThat(inserted.getImageUrl()).isEqualTo("https://example.com/api/T1/images/565812");
}

@Test
void listSync_doesNotOverwriteExistingImageUrl() throws Exception {
    Date avail = date(2026, 6, 1);
    House existing = house("E1", avail);
    existing.setImageUrl("https://existing-url.com/image.jpg");
    when(client.getAllPropertiesList()).thenReturn(
        List.of(listItemWithThumbnail("E1", avail, "f-999999"))
    );
    when(repository.findAllByEndDateIsNull()).thenReturn(List.of(existing));
    when(repository.findAllByExternalIds(any())).thenReturn(List.of(existing));

    service.syncHouseList();

    assertThat(existing.getImageUrl()).isEqualTo("https://existing-url.com/image.jpg");
}

// -------- New fields from detail --------

@Test
void detailApply_setsQueueType() throws Exception {
    House h = new House();
    HouseDetail detail = detailWithNewFields("residential", "Parkeringsplats", 5, List.of());

    invokeApplyDetail(h, detail);

    assertThat(h.getQueueType()).isEqualTo("residential");
}

@Test
void detailApply_setsRentalObjectType() throws Exception {
    House h = new House();
    HouseDetail detail = detailWithNewFields("parking", "Centralgarage", 0, List.of());

    invokeApplyDetail(h, detail);

    assertThat(h.getRentalObjectType()).isEqualTo("Centralgarage");
}

@Test
void detailApply_setsNrApplications() throws Exception {
    House h = new House();
    HouseDetail detail = detailWithNewFields("residential", null, 42, List.of());

    invokeApplyDetail(h, detail);

    assertThat(h.getNrApplications()).isEqualTo(42);
}

@Test
void detailApply_setsIncludedJsonFromList() throws Exception {
    House h = new House();
    HouseDetail detail = detailWithNewFields("residential", null, 0,
        List.of(new HouseIncluded("Balkong"), new HouseIncluded("Hiss")));

    invokeApplyDetail(h, detail);

    assertThat(h.getIncludedJson()).isNotBlank();
    assertThat(h.getIncludedJson()).contains("Balkong").contains("Hiss");
}

@Test
void detailApply_setsIncludedJsonNullWhenListNull() throws Exception {
    House h = new House();
    h.setIncludedJson("[{\"displayName\":\"Balkong\"}]");
    HouseDetail detail = detailWithNewFields("residential", null, 0, null);

    invokeApplyDetail(h, detail);

    // null included list results in null JSON (the field is fully replaced, not sticky)
    assertThat(h.getIncludedJson()).isNull();
}
```

Add these helpers at the bottom of the helpers section:

```java
private static HouseListItem listItemWithQueueType(String id, Date availableFrom, String queueType) {
    return new HouseListItem(
        id, "local-" + id, "display", "desc",
        queueType, queueType, null,
        new HousePricing(0.0), null,
        availableFrom == null ? null : new HouseAvailability(availableFrom),
        new HouseSize("1 rok", "1", 30.0),
        null
    );
}

private static HouseListItem listItemWithRentalObjectType(String id, Date availableFrom,
                                                           String queueType, String rentalObjectType) {
    return new HouseListItem(
        id, "local-" + id, "display", "desc",
        queueType, queueType, rentalObjectType,
        null, null,
        availableFrom == null ? null : new HouseAvailability(availableFrom),
        null,
        null
    );
}

private static HouseListItem listItemWithThumbnail(String id, Date availableFrom, String version) {
    return new HouseListItem(
        id, "local-" + id, "display", "desc",
        "residential", "residential", null,
        new HousePricing(0.0), null,
        availableFrom == null ? null : new HouseAvailability(availableFrom),
        new HouseSize("1 rok", "1", 30.0),
        new HouseThumbnail(true, "Bild", version)
    );
}

private static HouseDetail detailWithNewFields(String queueType, String rentalObjectType,
                                                Integer nrApplications, List<HouseIncluded> included) {
    return new HouseDetail(
        null, null, null, null, null,
        queueType, null, rentalObjectType, nrApplications,
        null, null, null, null, null, null,
        null,
        included
    );
}
```

Also add the imports needed at the top of `HouseSyncServiceTest`:

```java
import com.prasannjeet.vaxjobostader.client.dto.house.HouseIncluded;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseThumbnail;
import com.prasannjeet.vaxjobostader.jpa.ObjectTypeRepository;
import java.util.List;
```

Run to confirm these new tests fail:

```bash
mymvn test -Dtest=HouseSyncServiceTest --no-transfer-progress 2>&1 | tail -20
```

Expected: compilation errors because `ObjectTypeRepository` is not yet in `HouseSyncService`'s constructor and the new House getters don't exist yet.

- [ ] **Step 2: Update HouseSyncService**

Add the import block and the new field in `HouseSyncService`:

At the top of the class, add import:
```java
import com.prasannjeet.vaxjobostader.client.dto.house.HouseIncluded;
import com.prasannjeet.vaxjobostader.jpa.ObjectType;
import com.prasannjeet.vaxjobostader.jpa.ObjectTypeRepository;
import com.prasannjeet.vaxjobostader.client.dto.house.ObjectTypeMetadata;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
```

Add the new field after `addressGeocodeService`:
```java
private final ObjectTypeRepository objectTypeRepository;
```

Replace the body of `syncHouseList()` with the updated version. The full method (showing only the modified portions — integrate with the unchanged skeleton):

The reconcile loop inside the `if (!apiItems.isEmpty())` block gains three additional mappings per house. After `house.setDisplayName(item.displayName())`, add:

```java
house.setQueueType(item.queueType());
house.setRentalObjectType(item.rentalObjectType());
if (house.getImageUrl() == null
        && item.thumbnail() != null
        && item.thumbnail().exists()
        && item.thumbnail().version() != null
        && item.thumbnail().version().startsWith("f-")) {
    house.setImageUrl(
        appConfig.getVbUrl() + "/" + item.id() + "/images/"
            + item.thumbnail().version().substring(2)
    );
}
```

After `houseRepository.saveAll(toSave)` (the upsert save), add:

```java
Set<String> uniqueTypes = apiItems.stream()
    .map(HouseListItem::type)
    .filter(Objects::nonNull)
    .collect(Collectors.toSet());
syncTypeMetadata(uniqueTypes);
```

Add the new private method `syncTypeMetadata` at the end of the class (before the inner `HouseKey` record):

```java
private void syncTypeMetadata(Set<String> typeIds) {
    List<ObjectType> toSave = new ArrayList<>();
    for (String typeId : typeIds) {
        try {
            ObjectTypeMetadata meta = vaxjobostaderClient.getTypeMetadata(typeId);
            if (meta == null) continue;
            ObjectType entity = objectTypeRepository.findById(typeId)
                .orElseGet(() -> {
                    ObjectType o = new ObjectType();
                    o.setTypeId(typeId);
                    return o;
                });
            entity.setDisplayName(meta.displayName());
            entity.setDescription(meta.description());
            entity.setMinPrice(meta.minPrice());
            entity.setMaxPrice(meta.maxPrice());
            entity.setMinRooms(meta.minNumberOfRooms());
            entity.setMaxRooms(meta.maxNumberOfRooms());
            entity.setMinSize(meta.minSize());
            entity.setMaxSize(meta.maxSize());
            entity.setNumberOfMarketObjects(meta.numberOfMarketObjects());
            entity.setLastSyncedAt(Instant.now());
            toSave.add(entity);
        } catch (Exception e) {
            log.warn("Failed to sync metadata for type {}: {}", typeId, e.getMessage());
        }
    }
    if (!toSave.isEmpty()) {
        objectTypeRepository.saveAll(toSave);
    }
}
```

In `applyDetailToHouse()`, add after the existing `if (detail.type() != null)` block:

```java
if (detail.queueType() != null) house.setQueueType(detail.queueType());
if (detail.rentalObjectType() != null) house.setRentalObjectType(detail.rentalObjectType());
if (detail.nrApplications() != null) house.setNrApplications(detail.nrApplications());
house.setIncludedJson(toJsonOrNull(detail.included()));
```

- [ ] **Step 3: Run the full test suite**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -25
```

Expected: `BUILD SUCCESS` — all pre-existing tests pass, all new tests pass.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/com/prasannjeet/vaxjobostader/service/HouseSyncService.java \
        src/test/java/com/prasannjeet/vaxjobostader/service/HouseSyncServiceTest.java
git commit -m "feat: sync all property types — paginated list, type metadata, new house fields"
```

---

## Task 9: Final verification

- [ ] **Step 1: Full test suite and compile**

```bash
mymvn test --no-transfer-progress 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, zero test failures.

- [ ] **Step 2: Verify no references to the old method remain**

```bash
grep -rn "getPropertiesList\b" src/
```

Expected: no output (the old method is fully replaced).

- [ ] **Step 3: Verify the type=residential filter is gone**

```bash
grep -rn "type=residential" src/
```

Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add -A
git status
# Only commit if there are unstaged changes from the verification
git diff --exit-code || git commit -m "chore: final cleanup after all-types expansion"
```
