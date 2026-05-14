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
