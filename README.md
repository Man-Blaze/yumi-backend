# YumiExpress Backend

This is the backend engine for YumiExpress, a dedicated in-country and house-to-house delivery service in Port Vila, Vanuatu.

## Tech Stack
- **Language:** Java 17
- **Framework:** Spring Boot 4.1.1
- **Database:** H2 (Development) / PostgreSQL (Production)
- **Build Tool:** Maven

## Core Features (MVP)
- [x] **Pricing Engine:** Weight-banded pricing algorithm (500 VT to 1800 VT).
- [x] **Order Management API:** Endpoint to book a delivery and calculate the price dynamically.
- [ ] **Database Integration:** Saving orders permanently.
- [ ] **Dispatch Algorithm:** TSP and Knapsack logic for routing and rider assignment.
- [ ] **M-Vatu Payment Integration.**

## How to Run
1. Clone the repository.
2. Run `mvn spring-boot:run`.
3. The server starts on port 1000.

## Contact
Founder: Paul Riri Ishmael
