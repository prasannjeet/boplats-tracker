package com.prasannjeet.vaxjobostader.client.dto.house;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseLocation(HouseArea area, List<HouseAreaPath> areaPath, HouseAddress address, String floorDisplayName) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HouseAreaPath(String id, String displayName) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HouseAddress(String address, String street, String streetnumber, String postcode, String city, String country, String completeAdress) {}
}