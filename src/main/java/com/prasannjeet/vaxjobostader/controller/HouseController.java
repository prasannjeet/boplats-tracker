package com.prasannjeet.vaxjobostader.controller;

import com.prasannjeet.vaxjobostader.jpa.House;
import com.prasannjeet.vaxjobostader.jpa.HouseFloorplan;
import com.prasannjeet.vaxjobostader.jpa.HouseFloorplanRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseImage;
import com.prasannjeet.vaxjobostader.jpa.HouseImageRepository;
import com.prasannjeet.vaxjobostader.jpa.HouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/houses")
@RequiredArgsConstructor
public class HouseController {

    private final HouseRepository houseRepository;
    private final HouseImageRepository houseImageRepository;
    private final HouseFloorplanRepository houseFloorplanRepository;

    @GetMapping
    public List<House> list(@RequestParam(name = "includeEnded", defaultValue = "false") boolean includeEnded) {
        List<House> houses = includeEnded
            ? houseRepository.findAll()
            : houseRepository.findAllByEndDateIsNull();
        attachImagesAndFloorplans(houses);
        return houses;
    }

    private void attachImagesAndFloorplans(List<House> houses) {
        if (houses.isEmpty()) return;
        Set<String> externalIds = new HashSet<>(houses.size());
        for (House h : houses) {
            if (h.getId() != null) externalIds.add(h.getId());
        }
        if (externalIds.isEmpty()) return;

        Map<String, List<HouseImage>> imagesByHouse = houseImageRepository
            .findByHouseExternalIdInOrderBySortOrderAsc(externalIds)
            .stream()
            .collect(Collectors.groupingBy(HouseImage::getHouseExternalId));

        Map<String, List<HouseFloorplan>> floorplansByHouse = houseFloorplanRepository
            .findByHouseExternalIdInOrderBySortOrderAsc(externalIds)
            .stream()
            .collect(Collectors.groupingBy(HouseFloorplan::getHouseExternalId));

        for (House h : houses) {
            h.setImages(imagesByHouse.getOrDefault(h.getId(), Collections.emptyList()));
            h.setFloorplans(floorplansByHouse.getOrDefault(h.getId(), Collections.emptyList()));
        }
    }
}
