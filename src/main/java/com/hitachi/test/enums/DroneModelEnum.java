package com.hitachi.test.enums;

import lombok.Getter;

@Getter
public enum DroneModelEnum {
  LIGHTWEIGHT("LW", 0.5),
  MIDDLEWEIGHT("MW", 1),
  CRUISERWEIGHT("CW", 1.5),
  HEAVYWEIGHT("HW", 3);

  private final String code;
  private final double weightMultiplier;

  DroneModelEnum(String code, double weightMultiplier) {
    this.code = code;
    this.weightMultiplier = weightMultiplier;
  }
}
