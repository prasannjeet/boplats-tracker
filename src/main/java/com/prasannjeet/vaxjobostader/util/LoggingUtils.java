package com.prasannjeet.vaxjobostader.util;

import static lombok.AccessLevel.PRIVATE;

import lombok.NoArgsConstructor;
import org.slf4j.Logger;

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
