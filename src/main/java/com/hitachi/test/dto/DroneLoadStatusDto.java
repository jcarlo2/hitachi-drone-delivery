package com.hitachi.test.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter @Builder
public class DroneLoadStatusDto {
  private String message;
  private String droneSerialNumber;
  private Integer droneWeightCapacity;
  private String state;

  private String medicationCode;
  private Integer medicationQuantity;
  private Integer medicationWeight;

  private Integer currentWeight;
  private Integer totalWeight;
  private Integer remainingWeight;

  private String loadingProgress;
  private Boolean completed;
}
