package com.hitachi.test.service;

import com.hitachi.test.enums.DroneModelEnum;
import com.hitachi.test.model.Drone;

import java.util.Map;

public interface DummyRedisService {
  void saveDrone(Drone drone);
  long getDroneBatteryByRange(int max, int min);
  Map<DroneModelEnum, Long> getAvailableDroneByModel();
  Drone getDroneBySerialNumber(String serialNumber);
}
