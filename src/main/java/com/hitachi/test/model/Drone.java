package com.hitachi.test.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hitachi.test.dto.DroneSnapshot;
import com.hitachi.test.dto.LoadChecker;
import com.hitachi.test.enums.DroneModelEnum;
import com.hitachi.test.enums.DroneStateEnum;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.Semaphore;

import static com.hitachi.test.constant.Constant.defaultWeight;

@Entity
@Getter @ToString
public class Drone {
  @Getter(AccessLevel.NONE)
  private transient final Semaphore semaphore = new Semaphore(1);

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonIgnore
  Long id;

  @Getter(AccessLevel.NONE)
  private final String uuid;

  private final Integer weightLimit;

  @Enumerated(EnumType.STRING)
  private final DroneModelEnum model;

  @Setter
  @Enumerated(EnumType.STRING)
  @JsonIgnore
  private DroneStateEnum state;

  @Setter
  @JsonIgnore
  private Integer batteryPercentage;

  @JsonIgnore
  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Setter
  @JsonIgnore
  private transient LoadChecker loadChecker;

  public Drone(DroneModelEnum model) {
    this.model = model;
    uuid = UUID.randomUUID().toString();
    weightLimit = defaultWeight;
    batteryPercentage = 100;
    state = DroneStateEnum.IDLE;
    loadChecker = new LoadChecker();
  }

  protected Drone() {
    uuid = null;
    model = null;
    weightLimit = null;
  }

  public String getSerialNumber() {
    return String.format(
      "%020d-%s-%s",
      id,
      model.getCode(),
      uuid
    );
  }

  public DroneSnapshot snapshot() {
    return new DroneSnapshot(
      getSerialNumber(),
      batteryPercentage,
      weightLimit,
      state,
      loadChecker.getMedicationCode(),
      loadChecker.getMedicationQuantity(),
      loadChecker.getMedicationWeight(),
      loadChecker.getCurrentWeight()
    );
  }

  public void releasePermitLock() {
    semaphore.release();
  }

  public boolean checkPermitLock() {
    return semaphore.tryAcquire();
  }

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
