package com.prasannjeet.vaxjobostader.exception;

public class ClientException extends RuntimeException {

  public ClientException(String message, Throwable cause) {
    super(message, cause);
  }

  public ClientException(Throwable cause) {
    super("A Client Exception has occurred", cause);
  }

  public ClientException(String message) {
    super(message);
  }

}