package com.prasannjeet.vaxjobostader.jpa.converters;

import jakarta.persistence.AttributeConverter;

import static java.nio.charset.StandardCharsets.UTF_8;


public class StringLobConverter implements AttributeConverter<String, byte[]> {

  @Override
  public byte[] convertToDatabaseColumn(String attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return new byte[0];
    }
    return attribute.getBytes(UTF_8);
  }

  @Override
  public String convertToEntityAttribute(byte[] dbData) {
    if (dbData == null || dbData.length == 0) {
      return "";
    }
    return new String(dbData, UTF_8);
  }
}
