package com.prasannjeet.vaxjobostader.jpa;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "house_image")
public class HouseImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @JsonIgnore
    private Long id;

    @Column(name = "house_external_id", nullable = false, length = 255)
    @JsonIgnore
    private String houseExternalId;

    @Column(name = "display_name", length = 500)
    private String displayName;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "address", length = 1000)
    private String address;

    @Column(name = "linked_to_type", length = 100)
    private String linkedToType;

    @Column(name = "sort_order")
    @JsonIgnore
    private Integer sortOrder;
}
