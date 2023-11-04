package com.prasannjeet.vaxjobostader.jpa;

import com.prasannjeet.vaxjobostader.jpa.converters.ListLobConverter;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

@Entity
public class LastUpdated {

  @Id
  @Column(name = "id", nullable = false)
  private int id;

  @Column(name = "last_updated")
  private LocalDateTime lastUpdated;

  @Lob
  @Convert(converter = ListLobConverter.class)
  @Column(name = "preferred_objects")
  private List<String> preferredObjects;

}
