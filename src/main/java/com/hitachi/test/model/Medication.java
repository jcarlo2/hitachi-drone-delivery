package com.hitachi.test.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Medication {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  Long id;

  private String name;
  private String code;
  private Integer weight;
  private String path;

  @Column(nullable = false, updatable = false)
  private LocalDateTime createdAt;

  public Medication(String name, String code, Integer weight, String path) {
    this.name = name;
    this.code = code;
    this.weight = weight;
    this.path = path;
  }

  @PrePersist
  public void prePersist() {
    createdAt = LocalDateTime.now();
  }
}
