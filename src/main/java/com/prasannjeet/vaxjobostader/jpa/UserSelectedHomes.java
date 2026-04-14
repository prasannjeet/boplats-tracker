package com.prasannjeet.vaxjobostader.jpa;

import com.prasannjeet.vaxjobostader.jpa.converters.ListLobConverter;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

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
