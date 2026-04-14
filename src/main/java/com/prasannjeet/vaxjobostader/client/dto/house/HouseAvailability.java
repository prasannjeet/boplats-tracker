package com.prasannjeet.vaxjobostader.client.dto.house;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.prasannjeet.vaxjobostader.util.WcfDateDeserializer;
import java.util.Date;

@JsonIgnoreProperties(ignoreUnknown = true)
public record HouseAvailability(
    @JsonDeserialize(using = WcfDateDeserializer.class)
    Date availableFrom
) {}