package com.prasannjeet.vaxjobostader.util;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WcfDateDeserializer extends JsonDeserializer<Date> {
    private static final Pattern PATTERN = Pattern.compile("^/Date\\((\\d+)([+-]\\d{4})?\\)/$");

    @Override
    public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getValueAsString();
        if (value == null || value.isEmpty()) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(value);
        if (matcher.matches()) {
            long timeInMillis = Long.parseLong(matcher.group(1));
            return new Date(timeInMillis);
        }
        throw new IllegalArgumentException("Invalid WCF date format: " + value);
    }
}
