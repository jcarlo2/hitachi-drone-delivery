package com.hitachi.test.dto;

import com.hitachi.test.enums.CityEnum;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class DroneLoadDto {
  private CityEnum city;
  private String medicationCode;
  private Integer quantity;
}
