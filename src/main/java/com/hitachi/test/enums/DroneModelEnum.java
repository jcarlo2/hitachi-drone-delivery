package com.hitachi.test.enums;

import lombok.Getter;

@Getter
public enum DroneModelEnum {
  LIGHTWEIGHT("LW", 10),
  MIDDLEWEIGHT("MW", 20),
  CRUISERWEIGHT("CW", 30),
  HEAVYWEIGHT("HW", 40);

  private final String code;
  private final int capacity;

  DroneModelEnum(String code, int capacity) {
    this.code = code;
    this.capacity = capacity;
  }
}
