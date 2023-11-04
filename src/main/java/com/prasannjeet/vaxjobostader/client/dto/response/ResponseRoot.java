package com.prasannjeet.vaxjobostader.client.dto.response;

import static com.prasannjeet.vaxjobostader.util.LoggingUtils.logException;
import static com.prasannjeet.vaxjobostader.util.StaticUtils.getMapper;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.cloud.translate.Translate;
import com.google.cloud.translate.TranslateOptions;
import com.google.cloud.translate.Translation;
import com.prasannjeet.vaxjobostader.exception.ClientException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Slf4j
public final class ResponseRoot {

  private static final Translate TRANSLATE = TranslateOptions.getDefaultInstance().getService();
  @JsonProperty("Result")
  private List<Result> result;
  @JsonProperty("TotalCount")
  private int totalCount;
  @JsonProperty("ObjectMainGroupDescription")
  private String objectMainGroupDescription;
  @JsonProperty("ObjectMainGroupNo")
  private int objectMainGroupNo;

  @Override
  public String toString() {
    try {
      return getMapper().writeValueAsString(this);
    } catch (JsonProcessingException e) {
      logException(e, "Error while serializing RequestRoot Object to String", log);
      throw new ClientException(e);
    }
  }

  @SuppressWarnings("java:S3776")
  public void translateThis() {

    for (Result r : this.result) {
      if (r.getObjectTypeDescription() != null || r.getObjectTypeDescription().length() > 0) {
        r.setObjectTypeDescription(translate(r.getObjectTypeDescription()));
      }
      if (r.getObjectSubDescription() != null || r.getObjectSubDescription().length() > 0) {
        r.setObjectSubDescription(translate(r.getObjectSubDescription()));
      }
      if (r.getFirstInfoTextShort() != null || r.getFirstInfoTextShort().length() > 0) {
        r.setFirstInfoTextShort(translate(r.getFirstInfoTextShort()));
      }
      if (r.getFirstInfoText() != null || r.getFirstInfoText().length() > 0) {
        r.setFirstInfoText(translate(r.getFirstInfoText()));
      }
      if (r.getStatusDescriptionClient() != null || r.getStatusDescriptionClient().length() > 0) {
        r.setStatusDescriptionClient(translate(r.getStatusDescriptionClient()));
      }
      if (r.getSyndicateMarketPlaceImageAlt() != null
          || r.getSyndicateMarketPlaceImageAlt().length() > 0) {
        r.setSyndicateMarketPlaceImageAlt(translate(r.getSyndicateMarketPlaceImageAlt()));
      }
      if (r.getStatusDescription() != null || r.getStatusDescription().length() > 0) {
        r.setStatusDescription(translate(r.getStatusDescription()));
      }
    }

  }

  private String translate(String text) {
    Translation translation = TRANSLATE.translate(text);
    return translation.getTranslatedText();
  }

}
