package com.hitachi.test.exception;

public class DroneUnavailableException extends RuntimeException {
  public DroneUnavailableException(String message) {
    super(message);
  }
}