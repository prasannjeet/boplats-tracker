package com.prasannjeet.vaxjobostader.util;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT;
import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;
import static lombok.AccessLevel.PRIVATE;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@NoArgsConstructor(access = PRIVATE)
public class StaticUtils {

  private static final Logger LOG = LoggerFactory.getLogger(StaticUtils.class);

  public static ObjectMapper getMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
    mapper.configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
    mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    mapper.configure(FAIL_ON_EMPTY_BEANS, false);
    return mapper;
  }

}
