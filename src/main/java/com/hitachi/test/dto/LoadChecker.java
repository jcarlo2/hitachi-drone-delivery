package com.hitachi.test.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
public class LoadChecker {
  private Integer currentWeight;
  private String medicationCode;
  private Integer medicationWeight;
  private Integer medicationQuantity;
}
