package com.prasannjeet.vaxjobostader.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;
import static com.fasterxml.jackson.databind.DeserializationFeature.*;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class StaticUtils {

  public static ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    mapper.configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
    mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(FAIL_ON_EMPTY_BEANS, false);
    mapper.configure(ALLOW_COMMENTS, true);
    return mapper;
  }

}
