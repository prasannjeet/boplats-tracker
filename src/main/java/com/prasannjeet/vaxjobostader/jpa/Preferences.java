package com.prasannjeet.vaxjobostader.jpa;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class Preferences {

  @Id
  @Column(name = "id", nullable = false)
  private String id;

  @Column(name = "name")
  private String name;

  @Column(name = "webhook_url")
  private String webhookUrl;

  @Column(name = "min_rent")
  private int minRent;

  @Column(name = "max_rent")
  private int maxRent;

  @Column(name = "min_area")
  private int minArea;

  @Column(name = "max_area")
  private int maxArea;

  @Column(name = "q_points_main")
  private int qPointsMain;

  @Column(name = "q_points_main_date")
  private int qPointsMainDate;

  @Column(name = "q_points_student")
  private int qPointsStudent;

  @Column(name = "q_points_student_date")
  private int qPointsStudentDate;

  @Column(name = "min_rooms")
  private int minRooms;

  @Column(name = "max_rooms")
  private int maxRooms;

  @Column(name = "include_student")
  private boolean includeStudent;

  @Column(name = "include_senior")
  private boolean includeSenior;

  @Column(name = "include_external_companies")
  private boolean includeExternalCompanies;

}
