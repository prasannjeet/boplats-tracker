package com.prasannjeet.vaxjobostader.service.preferences;

import com.prasannjeet.vaxjobostader.enums.MarketPlaceDescription;
import com.prasannjeet.vaxjobostader.enums.PlaceName;
import com.prasannjeet.vaxjobostader.testbeans.Config;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Import(Config.class)
class HomeSearchConfigTest {

    @Autowired
    private List<HomeSearchConfig> homeSearchConfigList;

    @Test
    void homeSearchConfigListBeanCreationTest() {
        List<HomeSearchConfig> configList = homeSearchConfigList;
        assertThat(configList)
            .isNotNull()
            .isNotEmpty();

        // Example assertion for a known config in the JSON file
        HomeSearchConfig knownConfig = configList.stream()
            .filter(config -> "UniqueName".equals(config.name()))
            .findFirst()
            .orElse(null);

        assertThat(knownConfig)
            .isNotNull()
            .hasFieldOrPropertyWithValue("webHook", "https://hooks.slack.com/services/...")
            .hasFieldOrPropertyWithValue("marketPlaceDescriptions", EnumSet.of(MarketPlaceDescription.TORG, MarketPlaceDescription.SENIOR_TORG))
            .hasFieldOrPropertyWithValue("placeNames", EnumSet.of(PlaceName.VAXJO, PlaceName.VEDERSLOV));
    }

    @Test
    void homeSearchConfigListBeanCreationTestOtherFeatures() {
        List<HomeSearchConfig> configList = homeSearchConfigList;
        assertThat(configList)
            .isNotNull()
            .isNotEmpty();

        // Test for uniqueness of the "name" property
        assertThat(configList)
            .extracting(HomeSearchConfig::name)
            .doesNotHaveDuplicates();

        // Test that known valid config is present
        assertThat(configList)
            .extracting(HomeSearchConfig::name)
            .contains("UniqueName");

        // Test that invalid enum config is not present
        assertThat(configList)
            .extracting(HomeSearchConfig::name)
            .doesNotContain("InvalidEnumName");

        // Test that config with empty properties is not present
        assertThat(configList)
            .extracting(HomeSearchConfig::name)
            .doesNotContain("EmptyProperties");

        // Test that properties are not empty
        assertThat(configList)
            .filteredOn(config -> "UniqueName".equals(config.name()))
            .allSatisfy(this::assertValidConfigProperties);
    }

    private void assertValidConfigProperties(HomeSearchConfig config) {
        assertThat(config.webHook()).as("webHook should not be empty").isNotEmpty();
        assertThat(config.minRent()).as("minRent should be greater than 0").isPositive();
        assertThat(config.maxRent()).as("maxRent should be greater than 0").isPositive();
        assertThat(config.minArea()).as("minArea should be greater than 0").isPositive();
        assertThat(config.maxArea()).as("maxArea should be greater than 0").isPositive();
        assertThat(config.queuePoints()).as("queuePoints should be greater than 0").isPositive();

        // Check if queuePointsDate is a valid date in the format yyyyMMdd
        assertThat(config.queuePointsDate()).as("queuePointsDate should be a valid date")
            .matches(this::isValidDate, "Invalid date format, expected yyyyMMdd");

        assertThat(config.minRooms()).as("minRooms should be greater than or equal to 0").isNotNegative();
        assertThat(config.maxRooms()).as("maxRooms should be greater than 0").isPositive();
        assertThat(config.marketplace()).as("marketplace should be greater than 0").isPositive();
        assertThat(config.company()).as("company should be greater than 0").isPositive();
        assertThat(config.marketPlaceDescriptions()).as("marketPlaceDescriptions should not be empty").isNotEmpty();
        assertThat(config.placeNames()).as("placeNames should not be empty").isNotEmpty();
    }

    private boolean isValidDate(int date) {
        String dateString = Integer.toString(date);
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
        dateFormat.setLenient(false);
        try {
            dateFormat.parse(dateString);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }


}
