package com.hitachi.test.controller;

import com.hitachi.test.dto.DroneLoadDto;
import com.hitachi.test.dto.DroneLoadResponseDto;
import com.hitachi.test.dto.DroneLoadStatusDto;
import com.hitachi.test.dto.PayloadDto;
import com.hitachi.test.model.Drone;
import com.hitachi.test.service.DroneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.hitachi.test.constant.Constant.*;

@RestController
@RequestMapping("api/v1/drone")
public class DroneController {

  private final DroneService droneService;

  public DroneController(DroneService droneService) {
    this.droneService = droneService;
  }

  @PostMapping("/register")
  public ResponseEntity<Map<String, Object>> register(@RequestBody PayloadDto dto) {
    Drone drone = droneService.register(dto);
    if(drone == null) {
      return ResponseEntity.badRequest().body(Map.of(
        MESSAGE, "Invalid model of drone. Please choose between LIGHTWEIGHT, MIDDLEWEIGHT, CRUISERWEIGHT, HEAVYWEIGHT!"
      ));
    }
    return ResponseEntity.ok(Map.of(
      MESSAGE, "Drone registered successfully",
      DATA, drone
    ));
  }

  @PostMapping("/drone-load")
  public ResponseEntity<Map<String, Object>> droneLoad(@RequestBody DroneLoadDto dto) throws Exception {
    DroneLoadResponseDto response = droneService.droneLoad(dto);
    if(response.getDroneSerial() == null) {
      return ResponseEntity.internalServerError()
        .body(Map.of(MESSAGE, response.getMessage()));
    }
    return ResponseEntity.ok(Map.of(
      MESSAGE, "Drone is now loading medication",
      DRONE, response.getDroneSerial()
    ));
  }

  @GetMapping("/check-drone-status")
  public ResponseEntity<Map<String, Object>> checkDroneStatus(@RequestBody PayloadDto dto) {
    DroneLoadStatusDto loadStatusDto = droneService.checkDroneStatus(dto);
    if(loadStatusDto.getMessage() != null) {
      return ResponseEntity.internalServerError().body(Map.of(
        MESSAGE, loadStatusDto.getMessage()
      ));
    }
    return ResponseEntity.ok(Map.of(
      MESSAGE, "Drone status is generated successfully",
      DATA, loadStatusDto
    ));
  }

  @GetMapping("/check-drone-availability")
  public ResponseEntity<Map<String, Object>> checkDroneAvailability() {
    return ResponseEntity.ok(Map.of(
      MESSAGE, "Drone availability is generated successfully",
      DATA, droneService.checkDroneAvailability()
    ));
  }

  @GetMapping("/check-drone-battery")
  public ResponseEntity<Map<String, Object>> checkDroneBattery() {
    return ResponseEntity.ok(Map.of(
      MESSAGE, "Drone battery status is generated successfully",
      DATA, droneService.checkDroneBatteryStatus()
    ));
  }
}
