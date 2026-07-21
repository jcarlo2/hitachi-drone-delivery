package com.hitachi.test.service.impl;

import com.hitachi.test.dto.DroneLoadDto;
import com.hitachi.test.dto.DroneLoadResponseDto;
import com.hitachi.test.enums.CityEnum;
import com.hitachi.test.enums.DroneModelEnum;
import com.hitachi.test.model.Drone;
import com.hitachi.test.model.Medication;
import com.hitachi.test.repository.DroneRepository;
import com.hitachi.test.repository.MedicationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DroneServiceImplTest {
  @Mock
  private DroneRepository droneRepository;

  @Mock
  private MedicationRepository medicationRepository;

  @Mock
  private DummyRedisServiceImpl redisCache;

  @InjectMocks
  private DroneServiceImpl droneService;

  @Test
  void shouldReturnDroneAvailability() {
    Map<DroneModelEnum, Long> drones = Map.of(
      DroneModelEnum.LIGHTWEIGHT, 3L,
      DroneModelEnum.MIDDLEWEIGHT, 2L,
      DroneModelEnum.CRUISERWEIGHT, 1L,
      DroneModelEnum.HEAVYWEIGHT, 1L
    );

    when(redisCache.getAvailableDroneByModel()).thenReturn(drones);

    Map<String, Long> result = droneService.checkDroneAvailability();

    assertEquals(7L, result.get("Total"));
    assertEquals(3L, result.get("Lightweight"));
    assertEquals(2L, result.get("Middleweight"));
    assertEquals(1L, result.get("Cruiserweight"));
    assertEquals(1L, result.get("Heavyweight"));
  }

  @Test
  void shouldRegisterDrone() {
    Drone drone = new Drone(DroneModelEnum.LIGHTWEIGHT);
    when(droneRepository.save(any())).thenReturn(drone);
    Drone saved = droneRepository.save(drone);
    assertNotNull(saved);
    assertEquals(DroneModelEnum.LIGHTWEIGHT, saved.getModel());
    verify(droneRepository).save(any());
  }

  @Test
  void shouldReturnInvalidMedication() {
    DroneLoadDto dto = mock(DroneLoadDto.class);
    when(dto.getMedicationCode()).thenReturn("MED1");
    when(medicationRepository.findByCode("MED1"))
      .thenReturn(Optional.empty());

    DroneLoadResponseDto response = droneService.droneLoad(dto);
    assertEquals("Invalid Medication", response.getMessage());
  }

  @Test
  void shouldReturnNoIdleDrone() {
    DroneLoadDto dto = new DroneLoadDto();
    dto.setMedicationCode("MED1");

    Medication medication = new Medication();
    medication.setCode("MED1");

    when(medicationRepository.findByCode("MED1"))
      .thenReturn(Optional.of(medication));
    when(redisCache.getDroneMap())
      .thenReturn(Map.of());

    DroneLoadResponseDto response = droneService.droneLoad(dto);
    assertEquals("There are no available drones at the moment. Please try again later!", response.getMessage());
  }

  @Test
  void shouldReturnDroneBatteryRequirementIsNotMet() {
    DroneLoadDto dto = new DroneLoadDto();
    dto.setMedicationCode("MED1");
    dto.setCity(CityEnum.CITY_E);
    Drone drone = new Drone(DroneModelEnum.LIGHTWEIGHT);
    drone.setBatteryPercentage(20);

    when(medicationRepository.findByCode("MED1"))
      .thenReturn(Optional.of(new Medication("MED1", "MED1", 0, "")));
    when(redisCache.getDroneMap())
      .thenReturn(Map.of("D1", drone));

    DroneLoadResponseDto response = droneService.droneLoad(dto);
    assertEquals("All available drones do not meet the battery requirement.", response.getMessage()
    );
  }

  @Test
  void shouldReturnDroneWeightLimitExceed() {
    DroneLoadDto dto = new DroneLoadDto();
    dto.setMedicationCode("MED1");
    dto.setCity(CityEnum.CITY_E);
    dto.setQuantity(999);
    Drone drone = new Drone(DroneModelEnum.LIGHTWEIGHT);

    when(medicationRepository.findByCode("MED1"))
      .thenReturn(Optional.of(new Medication("MED1", "MED1", 9999, "")));
    when(redisCache.getDroneMap())
      .thenReturn(Map.of("D1", drone));

    DroneLoadResponseDto response = droneService.droneLoad(dto);
    assertEquals("The requested payload exceeds the maximum weight of all available drones.", response.getMessage()
    );
  }

  @Test
  void shouldReturnDroneItemCapacityExceed() {
    DroneLoadDto dto = new DroneLoadDto();
    dto.setMedicationCode("MED1");
    dto.setCity(CityEnum.CITY_E);
    dto.setQuantity(11);
    Drone drone = new Drone(DroneModelEnum.LIGHTWEIGHT);

    when(medicationRepository.findByCode("MED1"))
      .thenReturn(Optional.of(new Medication("MED1", "MED1", 1, "")));
    when(redisCache.getDroneMap())
      .thenReturn(Map.of("D1", drone));

    DroneLoadResponseDto response = droneService.droneLoad(dto);
    assertEquals("The requested payload exceeds the maximum capacity of all available drones.", response.getMessage()
    );
  }

  @Test
  void shouldReturnDroneLoadEdgeCase() {
    DroneLoadDto dto = new DroneLoadDto();
    dto.setMedicationCode("MED1");
    dto.setCity(CityEnum.CITY_E);
    dto.setQuantity(40); // maximum capacity of model
    Drone drone = new Drone(DroneModelEnum.HEAVYWEIGHT);
    drone.setBatteryPercentage(50); // required battery to travel to CITY_E

    when(medicationRepository.findByCode("MED1"))
      .thenReturn(Optional.of(new Medication("MED1", "MED1", 25, ""))); // edge case of weight [Quantity(40) * MedicationWeight(25) = 1000]
    when(redisCache.getDroneMap())
      .thenReturn(Map.of("D1", drone));

    DroneLoadResponseDto response = droneService.droneLoad(dto);
    assertEquals("Drone is now loading the medication ...", response.getMessage()
    );
  }

  @Test
  void shouldReturnSuccessfullLoading() {
    DroneLoadDto dto = new DroneLoadDto();
    dto.setMedicationCode("MED1");
    dto.setCity(CityEnum.CITY_E);
    dto.setQuantity(5);
    Drone drone = new Drone(DroneModelEnum.LIGHTWEIGHT);

    when(medicationRepository.findByCode("MED1"))
      .thenReturn(Optional.of(new Medication("MED1", "MED1", 1, "")));
    when(redisCache.getDroneMap())
      .thenReturn(Map.of("D1", drone));

    DroneLoadResponseDto response = droneService.droneLoad(dto);
    assertEquals("Drone is now loading the medication ...", response.getMessage()
    );
  }
}