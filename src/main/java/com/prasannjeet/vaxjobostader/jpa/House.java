package com.prasannjeet.vaxjobostader.jpa;

import com.fasterxml.jackson.annotation.JsonRawValue;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "house")
public class House {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "internal_id", nullable = false)
    private Long internalId;

    @Column(name = "id", nullable = false)
    private String id;

    @Column(name = "local_id")
    private String localId;

    @Column(name = "address")
    private String address;

    @Lob
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "rent")
    private Double rent;

    @Column(name = "area")
    private Double area;

    @Column(name = "rooms")
    private Integer rooms;

    @Temporal(TemporalType.DATE)
    @Column(name = "available_from")
    private Date availableFrom;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "application_deadline")
    private Date applicationDeadline;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Temporal(TemporalType.DATE)
    @Column(name = "end_date")
    private Date endDate;

    @Column(name = "queue_points")
    private Double queuePoints;

    @Column(name = "last_detail_fetched_at")
    private Instant lastDetailFetchedAt;

    @Column(name = "type", length = 100)
    private String type;

    @Column(name = "display_name", length = 500)
    private String displayName;

    @Column(name = "number", length = 100)
    private String number;

    @Column(name = "floor_display_name", length = 100)
    private String floorDisplayName;

    @Column(name = "area_name")
    private String areaName;

    @Column(name = "street")
    private String street;

    @Column(name = "street_number", length = 50)
    private String streetNumber;

    @Column(name = "postcode", length = 50)
    private String postcode;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "complete_address", length = 500)
    private String completeAddress;

    @JsonRawValue
    @Column(name = "area_path", columnDefinition = "json")
    private String areaPathJson;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // The address the latitude/longitude correspond to. Lets the detail
    // sync detect address changes cheaply: if the current completeAddress
    // equals geocodedAddress and coords are set, no work is needed.
    @Column(name = "geocoded_address", length = 500)
    private String geocodedAddress;

    @Column(name = "geocoded_at")
    private Instant geocodedAt;

    // Populated by the controller from the house_image / house_floorplan child
    // tables. Not persisted on House itself — keyed by external id, so the
    // same image rows belong to every re-listing of a given property.
    @Transient
    private List<HouseImage> images = Collections.emptyList();

    @Transient
    private List<HouseFloorplan> floorplans = Collections.emptyList();
}