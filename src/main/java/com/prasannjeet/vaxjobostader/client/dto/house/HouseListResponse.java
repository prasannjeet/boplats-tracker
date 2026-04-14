package com.prasannjeet.vaxjobostader.client.dto.house;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseListResponse(
    Integer count,
    List<HouseListItem> items
) {}