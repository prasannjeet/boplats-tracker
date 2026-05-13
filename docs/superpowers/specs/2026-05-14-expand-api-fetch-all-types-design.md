# Expand API Fetch to All Property Types

**Date:** 2026-05-14  
**Status:** Approved

## Background

The current `VaxjobostaderClient.getPropertiesList()` hardcodes `?type=residential&limit=1000`, which returns only ~100–169 residential listings. The Momentum API holds 1,297 active objects across seven distinct types. This spec covers expanding the fetch to all types, updating the data model to capture the new fields, and adding a type-metadata table.

## Scope

- Remove the `type=residential` filter; fetch all 1,297 items via paginated limit/offset
- Add `queue_type`, `rental_object_type`, `nr_applications`, and `included_json` columns to the `house` table
- New `object_type` table for type metadata (upserted after each full list sync)
- Update DTOs and sync service; detail fetch cadence is unchanged (self-adaptive)
- No frontend changes in this spec

## All Seven Types (from API research)

Stored as opaque IDs in the database. The metadata table provides human-readable names.

| `type` (raw API field) | Swedish name | English meaning | `queueType` | Active items |
|---|---|---|---|---|
| `residential` | Bostad | Standard apartments | `residential` | 169 |
| `fdRCFvqgMwjfyQP4Y8hRwF4W` | Poängfritt | No-queue / move-in ready | `residential` | 75 |
| `fQHrWQhP9kGBf8m7Mb6R88KB` | Seniorbostad | Senior housing (55+/70+) | `residential` | 10 |
| `PchcrTj8FVXJcCkxRFwMx7Qk` | Trygghetsboende | Safe housing (65+) | `residential` | 5 |
| `RwxTWw86hrYkBhPv4KhtBBrk` | Studentbostad | Student housing | `student` | 186 |
| `KPJGYD7TJGMjXRVJCmbmtb7d` | Ungdomsbostad | Youth housing (18–26) | `residential` | 0 |
| `parking` | Fordonsplats | Parking / garages | `parking` | 852 |

`queueType` is the reliable runtime discriminator (`residential` / `student` / `parking`). The raw `type` field is unreliable for categorisation — student housing uses an opaque ID there. Both are stored; `queue_type` is the one to filter on.

## API Client Layer

### `VaxjobostaderClient`

Replace `getPropertiesList()` with `getAllPropertiesList()`:

```
GET /market/objects?limit=25&offset=0
GET /market/objects?limit=25&offset=25
... until offset >= count
```

- `count` is read from the first response. Subsequent pages use it as the stop condition.
- A 200ms inter-page sleep keeps the load on the upstream API gentle.
- Returns `List<HouseListItem>` — the caller sees no pagination.
- Throws `IOException` on any non-200, consistent with the existing contract.

Constants (defined in the client):
```java
private static final int PAGE_SIZE = 25;
private static final long PAGE_FETCH_DELAY_MS = 200;
```

Add `getTypeMetadata(String typeId)`:
```
GET /market/objects/types/{typeId}
```
Returns `ObjectTypeMetadata`. Returns `null` on non-200 (type may be unknown to the API).

`getPropertyDetail(String id)` is unchanged.

## DTO Layer

### `HouseListItem` — new fields

```java
String queueType           // "residential" | "student" | "parking"
String rentalObjectType    // null for non-parking; e.g. "Parkeringsplats"
HouseThumbnail thumbnail   // {boolean exists, String displayName, String version}
```

`HouseThumbnail.version` format is `"f-{imageId}"`. Strip the `"f-"` prefix to get the image ID used in the URL `{BASE}/{itemId}/images/{imageId}`.

### `HouseDetail` — new fields

```java
String queueType
String queueTypeDisplayName   // e.g. "Bostad", "Fordonsplats"
String rentalObjectType
Integer nrApplications
List<HouseIncluded> included  // each: {String displayName}
```

`HouseIncluded` is a new record: `record HouseIncluded(String displayName) {}`.

### New `ObjectTypeMetadata` record

Maps the `/types/{typeId}` response:

```java
record ObjectTypeMetadata(
    String displayName,
    String description,
    Integer maxNumberOfApplications,
    Integer minNumberOfRooms, Integer maxNumberOfRooms,
    Double minSize, Double maxSize,
    Double minPrice, Double maxPrice,
    Integer numberOfMarketObjects
)
```

`singleMarketObjektDataFields` and `marketObjektListDataFields` are not persisted (returned all `"?"` names in research).

## Database

### Migration 1 — new columns on `house`

```xml
<changeSet id="house-add-type-columns" author="prasannjeet">
    <addColumn tableName="house">
        <column name="queue_type" type="VARCHAR(50)"/>
        <column name="rental_object_type" type="VARCHAR(255)"/>
        <column name="nr_applications" type="INT"/>
        <column name="included_json" type="JSON"/>
    </addColumn>
</changeSet>
```

All columns are nullable — rows inserted before this migration keep null values and will be updated on the next detail sync.

### Migration 2 — new `object_type` table

```xml
<changeSet id="create-object-type-table" author="prasannjeet">
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
```

Keyed by `type_id` (the raw API `type` value). Upserted after each list sync; the full row is replaced on every refresh. No historical retention needed.

## JPA Entities

### `House` — four new fields

```java
@Column(name = "queue_type", length = 50)
private String queueType;

@Column(name = "rental_object_type")
private String rentalObjectType;

@Column(name = "nr_applications")
private Integer nrApplications;

@Column(name = "included_json", columnDefinition = "json")
private String includedJson;
```

### New `ObjectType` entity

```java
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

    @Column(name = "min_price")  private Double minPrice;
    @Column(name = "max_price")  private Double maxPrice;
    @Column(name = "min_rooms")  private Integer minRooms;
    @Column(name = "max_rooms")  private Integer maxRooms;
    @Column(name = "min_size")   private Double minSize;
    @Column(name = "max_size")   private Double maxSize;
    @Column(name = "number_of_market_objects") private Integer numberOfMarketObjects;
    @Column(name = "last_synced_at") private Instant lastSyncedAt;
}
```

### New `ObjectTypeRepository`

```java
public interface ObjectTypeRepository extends JpaRepository<ObjectType, String> {}
```

Standard `save()` / `saveAll()` from `JpaRepository` is sufficient (upsert by PK).

## Sync Service

### `syncHouseList()` changes

`getAllPropertiesList()` now returns `List<HouseListItem>` directly. The healthy-response guard changes from checking `HouseListResponse != null` to checking `!apiItems.isEmpty()`.

1. Call `vaxjobostaderClient.getAllPropertiesList()` instead of the old single call
2. In the reconcile loop, additionally set per house:
   - `house.setQueueType(item.queueType())`
   - `house.setRentalObjectType(item.rentalObjectType())`
   - Pre-populate `imageUrl` from the thumbnail when `thumbnail != null && thumbnail.exists() == true` and `house.imageUrl == null`. The version field uses the format `"f-{imageId}"` (e.g. `"f-565245"`); strip the two-character `"f-"` prefix to get the image ID. Construct the URL as:
     ```
     imageId = item.thumbnail().version().substring(2)
     imageUrl = appConfig.getVbUrl() + "/" + item.id() + "/images/" + imageId
     ```
     Only sets it when `imageUrl` is currently null — the detail sync's full URL from `files.locationImage[0].address` takes precedence once it runs.
3. After `houseRepository.saveAll(toSave)`, collect the unique `type` IDs from the response and call `syncTypeMetadata(uniqueTypes)`.

### New `syncTypeMetadata(Set<String> typeIds)`

```java
private void syncTypeMetadata(Set<String> typeIds) {
    List<ObjectType> toSave = new ArrayList<>();
    for (String typeId : typeIds) {
        try {
            ObjectTypeMetadata meta = vaxjobostaderClient.getTypeMetadata(typeId);
            if (meta == null) continue;
            ObjectType entity = objectTypeRepository.findById(typeId)
                .orElseGet(() -> { ObjectType o = new ObjectType(); o.setTypeId(typeId); return o; });
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
    objectTypeRepository.saveAll(toSave);
}
```

Failure for one type does not abort the others.

### `applyDetailToHouse()` changes

Add mappings for the new detail fields:
- `house.setQueueType(detail.queueType())`
- `house.setRentalObjectType(detail.rentalObjectType())`
- `house.setNrApplications(detail.nrApplications())`
- `house.setIncludedJson(toJsonOrNull(detail.included()))`

`queuePointsCurrentPositionX` sticky logic is unchanged.

### Detail sync cadence

With 1,297 active items and a 12-hour refresh window:
```
perHouse = 43200 / 1297 ≈ 33s
```
This is just above the 30s minimum clamp. No changes to the adaptive cadence formula are needed.

### No backfilling

On the first sync after deployment, `syncHouseList()` fetches all 1,297 items and sets `queue_type` / `rental_object_type` / `imageUrl` for every row immediately. The detail sync fills in the remaining new fields within the first 12-hour window. No manual backfill script is required.

## Error Handling

- `getAllPropertiesList()` throws `IOException` on any HTTP error, consistent with existing contract. `syncHouseList()` catches and logs it — same as today.
- `getTypeMetadata()` returns `null` on non-200; `syncTypeMetadata()` silently skips null responses.
- Partial pagination failure in `getAllPropertiesList()` aborts the entire sync run (the exception propagates). The next scheduled run retries from offset 0. This is acceptable for a daily sync.

## Files Changed

| File | Change |
|---|---|
| `VaxjobostaderClient.java` | Replace `getPropertiesList()` → `getAllPropertiesList()`; add `getTypeMetadata()` |
| `HouseListItem.java` | Add `queueType`, `rentalObjectType`, `thumbnail` |
| `HouseThumbnail.java` | New record |
| `HouseDetail.java` | Add `queueType`, `queueTypeDisplayName`, `rentalObjectType`, `nrApplications`, `included` |
| `HouseIncluded.java` | New record |
| `ObjectTypeMetadata.java` | New record |
| `House.java` | Add 4 new fields |
| `ObjectType.java` | New entity |
| `ObjectTypeRepository.java` | New repository |
| `HouseSyncService.java` | Update `syncHouseList()`, `applyDetailToHouse()`; add `syncTypeMetadata()` |
| `house-type-columns.xml` | New Liquibase changeset — 4 columns on `house` |
| `object-type-table.xml` | New Liquibase changeset — `object_type` table |
| `db-changelog-master.xml` | Include the two new changeset files |
