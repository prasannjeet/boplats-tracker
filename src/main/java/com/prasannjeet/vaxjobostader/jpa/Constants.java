package com.prasannjeet.vaxjobostader.jpa;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

@Entity
@Table(name = "constants")
public class Constants {
    @Id
    @Column(name = "id", nullable = false)
    private int id;

    @Column(name = "last_database_updated")
    private LocalDateTime lastUpdated;
}
