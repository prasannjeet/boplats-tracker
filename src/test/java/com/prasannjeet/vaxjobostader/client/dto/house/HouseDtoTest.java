package com.prasannjeet.vaxjobostader.client.dto.house;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prasannjeet.vaxjobostader.util.StaticUtils;
import org.junit.jupiter.api.Test;
import java.io.InputStream;
import static org.assertj.core.api.Assertions.assertThat;

class HouseDtoTest {

    private final ObjectMapper mapper = StaticUtils.getMapper();

    @Test
    void testListResponseDeserialization() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/list-response.json")) {
            HouseListResponse response = mapper.readValue(is, HouseListResponse.class);
            assertThat(response).isNotNull();
            assertThat(response.count()).isEqualTo(153);
            assertThat(response.items()).hasSize(1); // There are 1 items in the list-response.json file

            HouseListItem firstItem = response.items().get(0);
            assertThat(firstItem.id()).isEqualTo("HFPvTcWGtGFRqJddx76BHbJb");
            assertThat(firstItem.localId()).isEqualTo("4430449");
            assertThat(firstItem.displayName()).isEqualTo("Hagtornsvägen 5 A");
            assertThat(firstItem.pricing().price()).isEqualTo(5035.6000);
            assertThat(firstItem.location().area().displayName()).isEqualTo("Åbyfors  2:81");
            assertThat(firstItem.availability().availableFrom()).isNotNull();
            assertThat(firstItem.size().roomsDisplayName()).isEqualTo("2 rum och kök");
            assertThat(firstItem.size().area()).isEqualTo(51.0000);
        }
    }

    @Test
    void testDetailResponseDeserialization() throws Exception {
        try (InputStream is = getClass().getResourceAsStream("/detail-response.json")) {
            HouseDetail detail = mapper.readValue(is, HouseDetail.class);
            assertThat(detail).isNotNull();
            assertThat(detail.id()).isEqualTo("HFPvTcWGtGFRqJddx76BHbJb");
            assertThat(detail.localId()).isEqualTo("4430449");
            assertThat(detail.number()).isEqualTo("AN-12936");
            assertThat(detail.pricing().price()).isEqualTo(5035.6000);
            assertThat(detail.location().address().street()).isEqualTo("Hagtornsvägen");
            assertThat(detail.location().floorDisplayName()).isEqualTo("Våning BV");
            assertThat(detail.availability().availableFrom()).isNotNull();
            assertThat(detail.application().openTo()).isNotNull();
            assertThat(detail.files().locationImage()).hasSize(2);
            assertThat(detail.files().locationImage().get(0).address()).contains("565812");
            assertThat(detail.files().floorplan()).hasSize(1);
            assertThat(detail.files().floorplan().get(0).displayName()).isEqualTo("033-001.pdf");
            assertThat(detail.queuePointsCurrentPositionX()).isNull(); // not present in fixture = null
        }
    }
}
