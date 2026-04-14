package com.prasannjeet.vaxjobostader.enums;

public enum MarketPlaceDescription {
    TORG("Torget"),
    SENIOR_TORG("Senior Torget"),
    DIREKT("Direkt"),
    NYBYGGN_TORG("Nybyggn. Torget"),
    STUDENT_TORG("Student Torget"),
    STUDENT_DIREKT("Student Direkt");

    private final String description;

    MarketPlaceDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}

