package com.prasannjeet.vaxjobostader.jpa;

import com.prasannjeet.vaxjobostader.jpa.converters.ListLobConverter;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
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
@Table(name = "user_selected_items")
public class UserSelectedHomes {

  @Id
  @Column(name = "id", nullable = false)
  private String id; // Changed from int to String

  @Lob
  @Convert(converter = ListLobConverter.class)
  @Column(name = "preferred_objects")
  private List<String> preferredObjects;

}
