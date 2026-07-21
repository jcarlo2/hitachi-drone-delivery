package com.hitachi.test.enums;

import lombok.Getter;

@Getter
public enum CityEnum {
  CITY_A(1),
  CITY_B(2),
  CITY_C(3),
  CITY_D(4),
  CITY_E(5);

  private final int distance;

  CityEnum(int distance) {
    this.distance = distance;
  }
}
