package com.hitachi.test.service.impl;

import com.hitachi.test.dto.*;
import com.hitachi.test.enums.CityEnum;
import com.hitachi.test.enums.DroneModelEnum;
import com.hitachi.test.enums.DroneStateEnum;
import com.hitachi.test.exception.DroneUnavailableException;
import com.hitachi.test.model.Drone;
import com.hitachi.test.model.Medication;
import com.hitachi.test.repository.DroneRepository;
import com.hitachi.test.repository.MedicationRepository;
import com.hitachi.test.service.DroneService;
import jakarta.transaction.Transactional;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static com.hitachi.test.constant.Constant.DEFAULT_DELIVERY_BATTERY_COST;
import static com.hitachi.test.constant.Constant.DEFAULT_DELIVERY_BATTERY_REQUIRED;

@Service
public class DroneServiceImpl implements DroneService {

  private final DroneRepository droneRepository;
  private final MedicationRepository medicationRepository;
  private final DummyRedisServiceImpl redisCache;

  public DroneServiceImpl(DroneRepository droneRepository, MedicationRepository medicationRepository, DummyRedisServiceImpl redisCache) {
    this.droneRepository = droneRepository;
    this.medicationRepository = medicationRepository;
    this.redisCache = redisCache;
  }

  @Override @Transactional
  public Drone register(PayloadDto dto) {
    try {
      DroneModelEnum model = DroneModelEnum.valueOf(dto.getDroneModel());
      Drone drone = new Drone(model);
      drone = droneRepository.save(drone);
      redisCache.saveDrone(drone);
      return drone;
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  @Override
  public Map<String, Long> checkDroneAvailability() {
    Map<DroneModelEnum, Long> availableDroneByModel = redisCache.getAvailableDroneByModel();
    return Map.of(
      "Total", availableDroneByModel.values().stream().mapToLong(Long::intValue).sum(),
      "Lightweight", availableDroneByModel.getOrDefault(DroneModelEnum.LIGHTWEIGHT, 0L),
      "Middleweight", availableDroneByModel.getOrDefault(DroneModelEnum.MIDDLEWEIGHT, 0L),
      "Cruiserweight", availableDroneByModel.getOrDefault(DroneModelEnum.CRUISERWEIGHT, 0L),
      "Heavyweight", availableDroneByModel.getOrDefault(DroneModelEnum.HEAVYWEIGHT, 0L)
    );
  }

  @Override
  public Map<String, Long> checkDroneBatteryStatus() {
    Long excellent = redisCache.getDroneBatteryByRange(100, 61);
    Long good = redisCache.getDroneBatteryByRange(60, 41);
    Long fair = redisCache.getDroneBatteryByRange(40, 25);
    Long low = redisCache.getDroneBatteryByRange(24, 0);
    return Map.of(
      "0%% - 24%", low,
      "25%% - 40%%", fair,
      "41%% - 60%%", good,
      "61%% - 100%%", excellent
    );
  }
  @Retryable(retryFor = Exception.class,  backoff = @Backoff(delay = 3000))
  @Override
  public DroneLoadResponseDto droneLoad(DroneLoadDto dto) {
    RetryContext context = RetrySynchronizationManager.getContext();
    Medication medication = medicationRepository.findByCode(dto.getMedicationCode()).orElse(null);
    if (medication == null) {
      return setDroneLoadResponse("Invalid Medication");
    }

    List<Drone> availableDrones = getIdleDrones();
    if (availableDrones.isEmpty()) {
      return setDroneLoadResponse("There are no available drones at the moment. Please try again later!");
    }

    availableDrones = filterByBatteryRequirement(availableDrones, dto);
    if (availableDrones.isEmpty()) {
      return setDroneLoadResponse("All available drones do not meet the battery requirement.");
    }

    availableDrones = filterByWeightLimit(availableDrones, dto, medication);
    if (availableDrones.isEmpty()) {
      return setDroneLoadResponse("The requested payload exceeds the maximum weight of all available drones.");
    }

    availableDrones = filterByCapacity(availableDrones, dto);
    if (availableDrones.isEmpty()) {
      return setDroneLoadResponse("The requested payload exceeds the maximum capacity of all available drones.");
    }

    Drone selectedDrone = lockDrone(availableDrones);
    if (selectedDrone == null) {
      if (context != null) {
        System.out.println("Retry attempt: " + context.getRetryCount());
      }
      String count = context == null ? "--" : String.valueOf(context.getRetryCount());
      throw new DroneUnavailableException(String.format("Retrying (%s) to find an available drone ...", count));
    }

    processDroneLoading(selectedDrone, dto, medication);
    DroneLoadResponseDto response = new DroneLoadResponseDto();
    response.setDroneSerial(selectedDrone.getSerialNumber());
    response.setMessage("Drone is now loading the medication ...");
    return response;
  }

  @Recover
  public DroneLoadResponseDto recover(DroneUnavailableException ignore, DroneLoadDto ignoreDto) {
    System.out.println("Retries exhausted ...");
    return setDroneLoadResponse("All drones that meet the delivery requirements are currently in use. Please try again later.");
  }

  private List<Drone> getIdleDrones() {
    return redisCache.getDroneMap()
      .values()
      .stream()
      .filter(drone -> DroneStateEnum.IDLE.equals(drone.getState()))
      .toList();
  }

  private List<Drone> filterByBatteryRequirement(List<Drone> drones, DroneLoadDto dto) {
    int requiredBattery = dto.getCity().getDistance() * DEFAULT_DELIVERY_BATTERY_COST;
    return drones.stream()
      .filter(drone ->
        drone.getBatteryPercentage() >= DEFAULT_DELIVERY_BATTERY_REQUIRED
          && drone.getBatteryPercentage() >= requiredBattery
      )
      .toList();
  }

  private List<Drone> filterByWeightLimit(List<Drone> drones, DroneLoadDto dto, Medication medication) {
    int requiredWeight = dto.getQuantity() * medication.getWeight();
    return drones.stream()
      .filter(drone -> drone.getWeightLimit() >= requiredWeight)
      .toList();
  }

  private List<Drone> filterByCapacity(List<Drone> drones, DroneLoadDto dto) {
    return drones.stream()
      .filter(drone -> drone.getModel().getCapacity() >= dto.getQuantity())
      .sorted(
        Comparator.comparing(Drone::getModel)
          .thenComparing(
            Comparator.comparingInt(Drone::getBatteryPercentage).reversed()
          )
      )
      .toList();
  }

  private Drone lockDrone(List<Drone> drones) {
    for (Drone drone : drones) {
      if (drone.checkPermitLock()) {
        return drone;
      }
    }
    return null;
  }

  private void processDroneLoading(Drone drone, DroneLoadDto dto, Medication medication) {
    try {
      drone.setState(DroneStateEnum.LOADING);
      CompletableFuture.runAsync(() -> {
        try {
          processMedication(drone, dto.getCity(), dto.getQuantity(), medication);
        } catch (Exception e) {
          drone.setState(DroneStateEnum.IDLE);
        } finally {
          drone.releasePermitLock();
        }
      });
    } catch (Exception e) {
      drone.setState(DroneStateEnum.IDLE);
      drone.releasePermitLock();
      throw e;
    }
  }

  private DroneLoadResponseDto setDroneLoadResponse(String message) {
    DroneLoadResponseDto response = new DroneLoadResponseDto();
    response.setMessage(message);
    return response;
  }

  @Override
  public DroneLoadStatusDto checkDroneStatus(PayloadDto dto) {
    if(dto.getDroneSerialNumber().isEmpty()) {
      return DroneLoadStatusDto.builder()
        .message("Drone serial number is empty!")
        .build();
    }
    Drone drone = redisCache.getDroneBySerialNumber(dto.getDroneSerialNumber());
    if(drone == null) {
      return DroneLoadStatusDto.builder()
        .message("Drone serial number is invalid. Please check the serial number and try again!")
        .build();
    }

    if(DroneStateEnum.IDLE.equals(drone.getState())) {
      return DroneLoadStatusDto.builder()
        .message("Drone is in idle state!")
        .build();
    }

    DroneSnapshot snapshot = drone.snapshot();
    int totalWeight = snapshot.getMedicationQuantity() * snapshot.getMedicationWeight();
    int currentWeight = snapshot.getCurrentWeight();
    return setDroneLoadStatusResponse(snapshot, currentWeight, totalWeight);
  }

  private static DroneLoadStatusDto setDroneLoadStatusResponse(DroneSnapshot snapshot, int currentWeight, int totalWeight) {
    return DroneLoadStatusDto.builder()
      .droneSerialNumber(snapshot.getSerialNumber())
      .droneWeightCapacity(snapshot.getWeightLimit())
      .state(snapshot.getState().name())
      .medicationCode(snapshot.getMedicationCode())
      .medicationQuantity(snapshot.getMedicationQuantity())
      .medicationWeight(snapshot.getMedicationWeight())
      .currentWeight(currentWeight)
      .totalWeight(totalWeight)
      .remainingWeight(Math.max(totalWeight - currentWeight, 0))
      .loadingProgress(
        totalWeight == 0
          ? "0%"
          : currentWeight * 100.0 / totalWeight + "%"
      )
      .completed(currentWeight >= totalWeight)
      .build();
  }

  private void processMedication(Drone drone, CityEnum city, Integer quantity, Medication medication) throws InterruptedException {
    int updatedBattery = calculateUpdatedBattery(drone, city);
    LoadChecker loadChecker = setLoadChecker(drone, quantity, medication.getWeight(), medication.getCode());

    stateTransition(drone, DroneStateEnum.LOADING, String.format("Drone %s: Loading medication...%n", drone.getSerialNumber()));
    loadMedication(quantity, medication, loadChecker);

    stateTransition(drone, DroneStateEnum.LOADED, String.format("Drone %s: Successfully loaded %d medication(s).%n", drone.getSerialNumber(), quantity));

    stateTransition(drone, DroneStateEnum.DELIVERING, String.format("Drone %s: Flying to %s...%n", drone.getSerialNumber(), city));

    stateTransition(drone, DroneStateEnum.DELIVERED, String.format("Drone %s: Successfully delivered the medication to %s.%n", drone.getSerialNumber(), city));
    setLoadChecker(drone, 0, 0, "");

    stateTransition(drone, DroneStateEnum.RETURNING, String.format("Drone %s: Returning to home base...%n", drone.getSerialNumber()));

    drone.setBatteryPercentage(updatedBattery);
    stateTransition(drone, DroneStateEnum.IDLE, String.format("Drone %s: Returned home. Battery: %d%%%n", drone.getSerialNumber(), drone.getBatteryPercentage()));

    droneRepository.save(drone);
    System.out.println("LAST STATE: " + drone.getState());
  }

  private void stateTransition(Drone drone, DroneStateEnum state, String message) throws InterruptedException {
    drone.setState(state);
    System.out.println(message);
    redisCache.saveDrone(drone);
    droneRepository.save(drone);
    Thread.sleep(1000);
  }

  private int calculateUpdatedBattery(Drone drone, CityEnum city) {
    int batteryCost = city.getDistance() * DEFAULT_DELIVERY_BATTERY_COST;
    return Math.max(drone.getBatteryPercentage() - batteryCost, 0);
  }

  private static void loadMedication(Integer quantity, Medication medication, LoadChecker loadChecker) throws InterruptedException {
    for (int i = 0; i < quantity; i++) {
      Thread.sleep(1000);
      loadChecker.setCurrentWeight(loadChecker.getCurrentWeight() + medication.getWeight());
    }
  }

  private static LoadChecker setLoadChecker(Drone drone, Integer quantity, Integer weight, String code) {
    LoadChecker loadChecker = drone.getLoadChecker();
    loadChecker.setMedicationCode(code);
    loadChecker.setMedicationQuantity(quantity);
    loadChecker.setMedicationWeight(weight);
    loadChecker.setCurrentWeight(0);
    return loadChecker;
  }
}
