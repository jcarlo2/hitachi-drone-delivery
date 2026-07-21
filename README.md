# Drone Medication Delivery System

## Overview

The Drone Medication Delivery System is a Spring Boot REST API that simulates a drone-based medication delivery service.

The application manages a fleet of drones capable of delivering medications while enforcing business rules such as battery validation, payload capacity, drone availability, and delivery state transitions.

This project was developed as part of the Drone Delivery coding assessment.

---

# Features

## Drone Management

- Register a new drone
- Check drone battery information
- Check drone availability by model
- Prevent duplicate drone registration

## Medication Delivery

- Load medication onto a drone
- Prevent drones from being overloaded
- Prevent loading when battery is below the required level
- Track medication loading progress
- Automatically update drone states during delivery
- Automatically reduce battery after delivery completion

## Drone Monitoring

- View current loading progress
- View loaded medication
- View drone battery percentage
- View available drones grouped by model

---

# Drone State Flow

```
IDLE
   │
   ▼
LOADING
   │
   ▼
LOADED
   │
   ▼
DELIVERING
   │
   ▼
DELIVERED
   │
   ▼
RETURNING
   │
   ▼
IDLE
```

---

# Drone Models

| Model         | Maximum Capacity |
|---------------|-----------------:|
| Lightweight   |               10 |
| Middleweight  |               20 |
| Cruiserweight |               30 |
| Heavyweight   |               40 |

> **Note:** Payload capacities are implementation assumptions as allowed by the assessment.

---

# Business Rules

- Maximum payload depends on the drone model.
- Drone battery must be at least **25%** before it can enter the **LOADING** state.
- Drone cannot exceed its payload capacity.
- Battery percentage is reduced after each completed delivery.
- Idle drones are automatically recharged by a scheduled task.
- Only one delivery request can reserve a drone at a time.

---

# Technology Stack

- Java 24
- Spring Boot 4
- Spring Web
- Spring Data JPA
- Spring Scheduler
- H2 In-Memory Database
- Maven
- Lombok

---

# Project Structure

```
src
├── main
│   ├── java
│   │   └── com.hitachi.test
│   │       ├── config          # Spring configuration
│   │       ├── constant        # Application constants
│   │       ├── controller      # REST Controllers
│   │       ├── dto             # Request & Response DTOs
│   │       ├── enums           # Enums (Drone Model, Drone State, etc.)
│   │       ├── initializer     # Loads dummy data
│   │       ├── model           # Domain models / Entities
│   │       ├── repository      # JPA repositories
│   │       ├── service         # Business logic
│   │       └── HitachiTestApplication.java
│   │
│   └── resources
│       └── image               # Medication images
```

---

# Application Design

## Data Storage

- H2 In-Memory Database is used for persistence.
- Initial drones and medications are automatically loaded during application startup.

## In-Memory Cache

The application uses an in-memory cache based on `ConcurrentHashMap` to simulate fast drone lookup and reservation.

## Concurrency

To prevent multiple requests from assigning the same drone simultaneously, each drone is protected using a **Semaphore**.

Only one thread may reserve a drone for delivery at a time.

## Scheduler

A Spring Scheduler automatically recharges idle drones every few seconds until they reach 100% battery.

---

# Dummy Data

The application automatically preloads sample data during startup.

## Drones

| Model         | Quantity | Payload Capacity |
|---------------|---------:|-----------------:|
| Lightweight   |        3 |               10 |
| Middleweight  |        2 |               20 |
| Cruiserweight |        3 |               30 |
| Heavyweight   |        2 |               40 |

---

## Medications

| Name        | Code   | Weight | Image           |
|-------------|--------|-------:|-----------------|
| Paracetamol | MED001 |   50 g | paracetamol.jpg |
| Amoxicillin | MED002 |   75 g | amoxicillin.png |
| Ibuprofen   | MED003 |   90 g | ibuprofen.jpeg  |

Medication images are located in:

```
src/main/resources/image
```

---

# REST APIs

## Register Drone

```
POST /drone/register
```
```json
{
  "data": {
    "model": "LIGHTWEIGHT",
    "serialNumber": "00000000000000000011-LW-bf9d9cec-d6ae-4d23-8dfd-bea5f9d8dedb",
    "weightLimit": 500
  },
  "message": "Drone registered successfully"
}
```
Registers a new drone.

---

## Load Medication

```
POST /drone/drone-load
```
```json
{
    "message": "Drone is now loading medication",
    "drone": "00000000000000000002-LW-7eb8a903-2871-4a5d-bf70-5f3fe358b5a5"
}
```

Assigns medication to an available drone.

Validation includes:

- Drone availability
- Battery level
- Payload capacity

---

## Check Drone Load Status

```
POST /drone/check-drone-status
```
```json
{
    "message": "Drone status is generated successfully",
    "data": {
        "completed": false,
        "currentWeight": 200,
        "droneSerialNumber": "00000000000000000003-LW-20259667-098f-4a44-b626-ab04422522f9",
        "droneWeightCapacity": 500,
        "loadingProgress": "40.0%",
        "medicationCode": "MED001",
        "medicationQuantity": 10,
        "medicationWeight": 50,
        "message": null,
        "remainingWeight": 300,
        "state": "LOADING",
        "totalWeight": 500
    }
}
```

Returns the current loading progress and medication information for a drone.

---

## Check Drone Availability

```
GET /drone/check-drone-availability
```
```json
{
  "message": "Drone availability is generated successfully",
  "data": {
    "Cruiserweight": 3,
    "Heavyweight": 2,
    "Total": 6,
    "Middleweight": 1,
    "Lightweight": 0
  }
}
```

Returns the number of available drones grouped by model.

---

## Check Drone Battery

```
POST /drone/check-drone-battery
```
```json
{
    "message": "Drone battery status is generated successfully",
    "data": {
        "51%% - 100%%": 0,
        "41%% - 50%%": 0,
        "0%% - 25%": 1,
        "26%% - 40%%": 0
    }
}
```

Returns the battery percentage of all drones.


---

# Running the Project

## Prerequisites

- Java 21
- Maven 3.9+

---

## Clone the Repository

```bash
  git clone https://github.com/jcarlo2/hitachi-drone-delivery.git
  cd hitachi-drone-delivery
```

---

## Build the Project

Build the project and download all required dependencies.

```bash
  mvn clean install
```

---

## Run the Application

Start the Spring Boot application using Maven:

```bash
  mvn spring-boot:run
```

Alternatively, after building the project, run the generated JAR:

```bash
  java -jar target/hitachi-test-0.0.1-SNAPSHOT.jar
```

> **Note:** Replace the JAR name if your generated artifact has a different version.

---

## Access the Application

Once the application is running, it will be available at:

```
http://localhost:8080
```

### H2 Database Console

```
http://localhost:8080/h2-console
```

Configuration

| Property | Value               |
|----------|---------------------|
| JDBC URL | jdbc:h2:mem:hitachi |
| Username | sa                  |
| Password | password            |

---

# Sample Delivery Lifecycle

```
Drone Selected
        │
        ▼
LOADING
        │
        ▼
Medication Loaded
        │
        ▼
DELIVERING
        │
        ▼
Medication Delivered
        │
        ▼
RETURNING
        │
        ▼
IDLE
        │
        ▼
Battery Recharge (Scheduler)
```

---

# Assumptions

- Drone communication with physical hardware is outside the scope.
- Authentication is not implemented.
- Images are stored locally under the resources folder.
- Drone delivery is simulated using asynchronous processing.
- Battery recharge is simulated using Spring Scheduler.
- Payload capacities per drone model are implementation assumptions.

---


# Author

**John Carlo Vendiola**