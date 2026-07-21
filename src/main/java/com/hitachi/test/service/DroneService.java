package com.hitachi.test.service;

import com.hitachi.test.dto.DroneLoadDto;
import com.hitachi.test.dto.DroneLoadResponseDto;
import com.hitachi.test.dto.DroneLoadStatusDto;
import com.hitachi.test.dto.PayloadDto;
import com.hitachi.test.model.Drone;

import java.util.Map;

public interface DroneService {

  Drone register(PayloadDto dto);

  Map<String, Long> checkDroneAvailability();

  Map<String, Long> checkDroneBatteryStatus();

  DroneLoadResponseDto droneLoad(DroneLoadDto dto) throws Exception;

  DroneLoadStatusDto checkDroneStatus(PayloadDto dto);
}
