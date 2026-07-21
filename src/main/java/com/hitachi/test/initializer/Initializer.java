package com.hitachi.test.initializer;

import com.hitachi.test.enums.DroneModelEnum;
import com.hitachi.test.enums.DroneStateEnum;
import com.hitachi.test.model.Drone;
import com.hitachi.test.model.Medication;
import com.hitachi.test.repository.DroneRepository;
import com.hitachi.test.repository.MedicationRepository;
import com.hitachi.test.service.DummyRedisService;
import com.hitachi.test.service.impl.DummyRedisServiceImpl;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
public class Initializer {
  private final DroneRepository droneRepository;
  private final MedicationRepository medicationRepository;
  private final DummyRedisService redisService;
  private final Executor executor = CompletableFuture.delayedExecutor(5000, TimeUnit.MILLISECONDS);
  final int BATTERY_RECHARGE_COUNT = 5;

  public Initializer(DroneRepository droneRepository, MedicationRepository medicationRepository, DummyRedisService redisService) {
    this.droneRepository = droneRepository;
    this.medicationRepository = medicationRepository;
    this.redisService = redisService;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void readyEvent() {
    List<Drone> droneList = List.of(
      new Drone(DroneModelEnum.LIGHTWEIGHT),
      new Drone(DroneModelEnum.LIGHTWEIGHT),
      new Drone(DroneModelEnum.LIGHTWEIGHT),
      new Drone(DroneModelEnum.MIDDLEWEIGHT),
      new Drone(DroneModelEnum.MIDDLEWEIGHT),
      new Drone(DroneModelEnum.CRUISERWEIGHT),
      new Drone(DroneModelEnum.CRUISERWEIGHT),
      new Drone(DroneModelEnum.CRUISERWEIGHT),
      new Drone(DroneModelEnum.HEAVYWEIGHT),
      new Drone(DroneModelEnum.HEAVYWEIGHT)
    );

    droneRepository.saveAll(droneList)
      .forEach(redisService::saveDrone);


    List<Medication> medicationList = List.of(
      new Medication(
        "Paracetamol",
        "MED001",
        50,
        "paracetamol.jpg"
      ),
      new Medication(
        "Amoxicillin",
        "MED002",
        75,
        "amoxicillin.jpg"
      ),
      new Medication(
        "Ibuprofen",
        "MED003",
        90,
        "ibuprofen.jpg"
      )
    );
    medicationRepository.saveAll(medicationList);
  }

  @Scheduled(fixedDelay = 100000)
  public void scheduleBatteryChecker() {
    List<Drone> fullyChargedDrones = new ArrayList<>();
    ((DummyRedisServiceImpl)redisService).getDroneMap()
      .values()
      .forEach(drone -> {
        if(DroneStateEnum.IDLE.equals(drone.getState()) && drone.getBatteryPercentage() < 100) {
          int updatedBatteryPercentage = drone.getBatteryPercentage() + BATTERY_RECHARGE_COUNT;
          drone.setBatteryPercentage(Math.min(100, updatedBatteryPercentage));
          redisService.saveDrone(drone);

          if(updatedBatteryPercentage >= 100) {
            fullyChargedDrones.add(drone);
          }
        }
      });

    if(!fullyChargedDrones.isEmpty()) {
      droneRepository.saveAll(fullyChargedDrones);
      System.out.println("Batch updated " + fullyChargedDrones.size() + " fully charged drones to DB.");
    }
  }
}
