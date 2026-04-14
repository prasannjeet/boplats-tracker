package com.prasannjeet.vaxjobostader.util;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public class LoggingUtils {

  public static void logException(Exception e, String errorMessage, Logger logger) {
    if (logger.isErrorEnabled()) {
      String message = e.getCause() != null ? e.getCause().toString() : "";
      if (errorMessage != null && !errorMessage.isEmpty()) {
        message = errorMessage + " " + message;
      }
      logger.error(message, e);
    }
  }

  public static void logException(Exception e, Logger logger) {
    logException(e, null, logger);
  }

}
