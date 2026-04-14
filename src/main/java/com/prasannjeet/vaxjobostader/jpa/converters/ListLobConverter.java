package com.prasannjeet.vaxjobostader.jpa.converters;

import jakarta.persistence.AttributeConverter;

import java.util.Arrays;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptyList;

public class ListLobConverter implements AttributeConverter<List<String>, byte[]> {

  @Override
  public byte[] convertToDatabaseColumn(List<String> attribute) {
    if (attribute == null || attribute.isEmpty()) {
      return new byte[0];
    }
    StringBuilder sb = new StringBuilder();
    for (String s : attribute) {
      sb.append(s).append(";");
    }
    sb.deleteCharAt(sb.length() - 1);
    return sb.toString().getBytes(UTF_8);
  }

  @Override
  public List<String> convertToEntityAttribute(byte[] dbData) {
    if (dbData == null || dbData.length == 0) {
      return emptyList();
    }
    return Arrays.asList(new String(dbData, UTF_8).split(";"));
  }
}
