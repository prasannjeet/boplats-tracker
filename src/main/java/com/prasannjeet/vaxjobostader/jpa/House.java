package com.prasannjeet.vaxjobostader.jpa;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "house")
public class House {

    @Id
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
}