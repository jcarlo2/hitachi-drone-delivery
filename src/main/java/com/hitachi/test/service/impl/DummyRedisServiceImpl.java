package com.hitachi.test.service.impl;

import com.hitachi.test.enums.DroneModelEnum;
import com.hitachi.test.enums.DroneStateEnum;
import com.hitachi.test.model.Drone;
import com.hitachi.test.service.DummyRedisService;
import lombok.Getter;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.hitachi.test.constant.Constant.DEFAULT_DELIVERY_BATTERY_REQUIRED;

@Repository
@Getter
public class DummyRedisServiceImpl implements DummyRedisService {
  private final Map<String, Drone> droneMap = new ConcurrentHashMap<>();

  @Override
  public void saveDrone(Drone drone) {
    droneMap.put(drone.getSerialNumber(), drone);
  }

  @Override
  public Drone getDroneBySerialNumber(String serialNumber) {
    return droneMap.get(serialNumber);
  }

  @Override
  public Map<DroneModelEnum, Long> getAvailableDroneByModel() {
    return droneMap.values()
      .stream()
      .filter(d ->
        d.getState().equals(DroneStateEnum.IDLE)
          && d.getBatteryPercentage() >= DEFAULT_DELIVERY_BATTERY_REQUIRED
      )
      .collect(Collectors.groupingBy(Drone::getModel,Collectors.counting()));
  }

  @Override
  public long getDroneBatteryByRange(int max, int min) {
    return droneMap.values()
      .stream()
      .filter(d ->
        d.getBatteryPercentage() >= min
        && d.getBatteryPercentage() <= max
      )
      .count();
  }
}
