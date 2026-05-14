package com.prasannjeet.vaxjobostader.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.client.VaxjobostaderClient;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseAvailability;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseDetail;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseIncluded;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseListItem;
import com.prasannjeet.vaxjobostader.client.dto.house.HousePricing;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseSize;
import com.prasannjeet.vaxjobostader.client.dto.house.HouseThumbnail;
import com.prasannjeet.vaxjobostader.config.AppConfig;
import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseFloorplanRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseImageRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import com.prasannjeet.vaxjobostader.jpa.ObjectTypeRepository;
import com.prasannjeet.vaxjobostader.service.geocoding.AddressGeocodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseSyncServiceTest {

    @Mock VaxjobostaderClient client;
    @Mock HouseRepository repository;
    @Mock HouseImageRepository imageRepository;
    @Mock HouseFloorplanRepository floorplanRepository;
    @Mock TaskScheduler taskScheduler;
    @Mock AddressGeocodeService addressGeocodeService;
    @Mock ObjectTypeRepository objectTypeRepository;

    AppConfig appConfig;

    @InjectMocks HouseSyncService service;

    @BeforeEach
    void setup() throws Exception {
        appConfig = new AppConfig();
        appConfig.setListApiCacheDurationHours(24);
        appConfig.setDetailRefreshIntervalHours(12);
        appConfig.setDetailFetchMinDelaySeconds(30);
        appConfig.setDetailFetchMaxDelaySeconds(1800);
        appConfig.setDetailFetchIdleDelaySeconds(1800);
        // re-create with the prepared appConfig
        service = new HouseSyncService(
            client, repository, imageRepository, floorplanRepository,
            appConfig, taskScheduler, new ObjectMapper(), addressGeocodeService,
            objectTypeRepository
        );
        // syncHouseList() always calls getTypeMetadata; return null by default to skip saving.
        lenient().when(client.getTypeMetadata(any())).thenReturn(null);
    }

    // -------- Sticky queue points --------

    @Test
    void queuePoints_writtenWhenIncomingNonNull() throws Exception {
        House h = new House();
        HouseDetail detail = detailWithQueuePoints(42.0);

        invokeApplyDetail(h, detail);

        assertThat(h.getQueuePoints()).isEqualTo(42.0);
    }

    @Test
    void queuePoints_preservedWhenIncomingNull() throws Exception {
        House h = new House();
        h.setQueuePoints(42.0);
        HouseDetail detail = detailWithQueuePoints(null);

        invokeApplyDetail(h, detail);

        assertThat(h.getQueuePoints()).isEqualTo(42.0);
    }

    @Test
    void queuePoints_overwrittenByNewerNonNull() throws Exception {
        House h = new House();
        h.setQueuePoints(10.0);
        HouseDetail detail = detailWithQueuePoints(99.0);

        invokeApplyDetail(h, detail);

        assertThat(h.getQueuePoints()).isEqualTo(99.0);
    }

    @Test
    void queuePoints_remainsNullWhenNeverSet() throws Exception {
        House h = new House();
        HouseDetail detail = detailWithQueuePoints(null);

        invokeApplyDetail(h, detail);

        assertThat(h.getQueuePoints()).isNull();
    }

    // -------- Composite-key reconcile --------

    @Test
    void reconcile_insertsNewListing() throws Exception {
        Date avail = date(2026, 6, 1);
        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
        when(repository.findAllByExternalIds(any())).thenReturn(List.of());

        service.syncHouseList();

        ArgumentCaptor<List<House>> saved = listCaptor();
        verify(repository, atLeastOnce()).saveAll(saved.capture());
        House inserted = saved.getAllValues().stream()
            .flatMap(List::stream).filter(h -> "A".equals(h.getId())).findFirst().orElseThrow();
        assertThat(inserted.getId()).isEqualTo("A");
        assertThat(inserted.getAvailableFrom()).isEqualTo(avail);
        assertThat(inserted.getEndDate()).isNull();
    }

    @Test
    void reconcile_updatesExistingActiveOnMatch() throws Exception {
        Date avail = date(2026, 6, 1);
        House existing = house("A", avail);
        existing.setRent(100.0);

        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail, 250.0)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of(existing));
        when(repository.findAllByExternalIds(any())).thenReturn(List.of(existing));

        service.syncHouseList();

        assertThat(existing.getRent()).isEqualTo(250.0);
        assertThat(existing.getEndDate()).isNull();
    }

    @Test
    void reconcile_updatesExistingEndedOnMatchAndClearsEndDate() throws Exception {
        Date avail = date(2026, 6, 1);
        House existing = house("A", avail);
        existing.setEndDate(date(2026, 6, 2));
        existing.setRent(100.0);

        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail, 250.0)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
        when(repository.findAllByExternalIds(any())).thenReturn(List.of(existing));

        service.syncHouseList();

        assertThat(existing.getRent()).isEqualTo(250.0);
        assertThat(existing.getEndDate()).isNull();

        ArgumentCaptor<List<House>> saved = listCaptor();
        verify(repository, atLeastOnce()).saveAll(saved.capture());
        assertThat(saved.getAllValues().stream().flatMap(List::stream))
            .contains(existing);
    }

    @Test
    void reconcile_endsActiveRowsMissingFromApi() throws Exception {
        Date avail = date(2026, 6, 1);
        House gone = house("GONE", avail);

        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("OTHER", avail)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of(gone));
        when(repository.findAllByExternalIds(any())).thenReturn(List.of());

        service.syncHouseList();

        assertThat(gone.getEndDate()).isNotNull();
    }

    @Test
    void reconcile_sameIdDifferentAvailableFromIsNewRow() throws Exception {
        Date oldAvail = date(2024, 1, 1);
        Date newAvail = date(2026, 6, 1);
        House existingActive = house("A", oldAvail);

        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", newAvail)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of(existingActive));
        when(repository.findAllByExternalIds(any())).thenReturn(List.of(existingActive));

        service.syncHouseList();

        // Old row gets ended (missing under its own (id, oldAvail) key)
        assertThat(existingActive.getEndDate()).isNotNull();

        // saveAll should have been called with a fresh House for the new key
        ArgumentCaptor<List<House>> saved = listCaptor();
        verify(repository, atLeastOnce()).saveAll(saved.capture());
        boolean newRowInserted = saved.getAllValues().stream().flatMap(List::stream)
            .anyMatch(h -> h.getId().equals("A")
                && h.getAvailableFrom().equals(newAvail)
                && h != existingActive);
        assertThat(newRowInserted).isTrue();
    }

    @Test
    void reconcile_skipsItemsWithNullAvailableFrom() throws Exception {
        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("BAD", null)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
        when(repository.findAllByExternalIds(any())).thenReturn(List.of());

        service.syncHouseList();

        // Nothing valid to save; saveAll for the upsert list gets called with an empty list.
        verify(repository, atLeastOnce()).saveAll(anyList());
    }

    @Test
    void reconcile_invokesPastDeadlineSweep() throws Exception {
        Date avail = date(2026, 6, 1);
        when(client.getAllPropertiesList()).thenReturn(List.of(listItem("A", avail)));
        when(repository.findAllByEndDateIsNull()).thenReturn(List.of());
        when(repository.findAllByExternalIds(any())).thenReturn(List.of());

        service.syncHouseList();

        verify(repository, times(1)).markPastDeadlineEnded(any(LocalDate.class), any(Date.class));
    }

    @Test
    void reconcile_runsDeadlineSweepEvenWhenApiResponseEmpty() throws Exception {
        when(client.getAllPropertiesList()).thenReturn(List.of());

        service.syncHouseList();

        // Reconcile is skipped...
        verify(repository, never()).findAllByEndDateIsNull();
        // ...but the deadline sweep still runs, since DB state shouldn't depend on API health.
        verify(repository, times(1)).markPastDeadlineEnded(any(LocalDate.class), any(Date.class));
    }

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

    // -------- Adaptive delay --------

    @Test
    void delay_idleWhenNoActiveHouses() {
        when(repository.countByEndDateIsNull()).thenReturn(0L);

        Duration d = service.computeNextDelay();

        assertThat(d).isEqualTo(Duration.ofSeconds(1800));
    }

    @Test
    void delay_clampsToMaxWhenWindowOverActiveExceedsMax() {
        // window = 12h = 43200s; with 1 active, raw = 43200, clamped to max=1800
        when(repository.countByEndDateIsNull()).thenReturn(1L);

        Duration d = service.computeNextDelay();

        assertThat(d).isEqualTo(Duration.ofSeconds(1800));
    }

    @Test
    void delay_clampsToMinWhenManyActive() {
        // window=43200s / 100000 active = 0 raw → max(min, raw) = min
        when(repository.countByEndDateIsNull()).thenReturn(100_000L);

        Duration d = service.computeNextDelay();

        assertThat(d).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void delay_proportionalInTheMiddle() {
        // 12h / 50 active = 864s; within [30, 1800] so passes through.
        when(repository.countByEndDateIsNull()).thenReturn(50L);

        Duration d = service.computeNextDelay();

        assertThat(d).isEqualTo(Duration.ofSeconds(864));
    }

    // -------- Helpers --------

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

    private static House house(String id, Date availableFrom) {
        House h = new House();
        h.setId(id);
        h.setAvailableFrom(availableFrom);
        return h;
    }

    private static HouseDetail detailWithQueuePoints(Double qp) {
        return new HouseDetail(
            null, null, null, null, null,
            null, null, null, null,
            null, null, null, null, null, null,
            qp,
            null
        );
    }

    private static Date date(int y, int m, int d) {
        return Date.from(LocalDate.of(y, m, d).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static <T> ArgumentCaptor<List<T>> listCaptor() {
        @SuppressWarnings({"unchecked", "rawtypes"})
        ArgumentCaptor<List<T>> c = (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
        return c;
    }

    private void invokeApplyDetail(House h, HouseDetail d) throws Exception {
        Method m = HouseSyncService.class.getDeclaredMethod("applyDetailToHouse", House.class, HouseDetail.class);
        m.setAccessible(true);
        m.invoke(service, h, d);
    }

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
}
