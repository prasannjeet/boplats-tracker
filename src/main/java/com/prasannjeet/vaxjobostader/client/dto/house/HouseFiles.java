package com.prasannjeet.vaxjobostader.client.dto.house;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseFiles(List<HouseImage> locationImage, List<HouseFloorplan> floorplan) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HouseImage(String displayName, String mimeType, String address, String linkedToType) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HouseFloorplan(String displayName, String mimeType, String address) {}
}