package com.prasannjeet.vaxjobostader.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
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
@Table(name = "address_geocode")
public class AddressGeocode {

    public enum Status { SUCCESS, NOT_FOUND }

    @Id
    @Column(name = "address", nullable = false, length = 500)
    private String address;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "provider", nullable = false, length = 50)
    private String provider;

    @Column(name = "geocoded_at", nullable = false)
    private Instant geocodedAt;
}
