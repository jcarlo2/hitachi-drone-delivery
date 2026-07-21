package com.hitachi.test.dto;

import com.hitachi.test.enums.DroneStateEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter @ToString
@AllArgsConstructor
public class DroneSnapshot {
  private final String serialNumber;
  private final Integer batteryPercentage;
  private final Integer weightLimit;
  private final DroneStateEnum state;

  private final String medicationCode;
  private final Integer medicationQuantity;
  private final Integer medicationWeight;
  private final Integer currentWeight;
}
