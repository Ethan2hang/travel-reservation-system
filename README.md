# Travel Reservation System

A full-stack flight reservation system built with Java Swing, MySQL, and JDBC. Three user roles (Admin, Customer Representative, Customer) interact with a normalized 13-table relational database through role-aware service-layer access control.

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)](https://www.mysql.com/)
[![Maven](https://img.shields.io/badge/Maven-3.9-red.svg)](https://maven.apache.org/)
[![License: MIT](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

## Features

### Customer
- 🔍 Flight search across two airports — one-way, round-trip, or flexible dates (±3 days)
- 📊 Sort and filter by price, departure/arrival time, duration, airline, number of stops
- 🎫 Book tickets with capacity check + automatic waitlist enrollment when full
- 🧾 View past and upcoming reservations with full segment details
- ↩️ Cancel reservations (Business / First class only — Economy is non-refundable)
- 🔔 Receive in-app alerts when a waitlisted seat becomes available
- ❓ Submit questions to a customer representative

### Customer Representative
- 📅 Make / edit reservations on behalf of customers
- ✏️ Update class, seat, and meal selections (with automatic fare recomputation)
- 🛠️ Full CRUD on aircraft, airports, and flights
- 👥 View waitlist passengers for any flight
- 🛬 List all flights for a given airport (departing and arriving)
- 💬 Reply to customer inquiries

### Admin
- 👤 Add / edit / delete customer reps and customers
- 📈 Sales report by month with detailed ticket breakdowns
- 🔎 Look up reservations by flight number or by customer name
- 💰 Revenue summaries by flight, airline, or customer
- 🏆 Identify top-revenue customer and most active flights
- 🛡️ Service-layer role enforcement (UI hiding alone is not sufficient)

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 (records, sealed types, switch expressions) |
| UI | Java Swing |
| Database | MySQL 8.0 |
| Persistence | Raw JDBC + hand-written DAO pattern (no ORM) |
| Build | Apache Maven 3.9 with the Shade plugin for fat-jar packaging |

**Zero runtime dependencies beyond `mysql-connector-j`.** No Spring, no Hibernate, no JPA. The choice is deliberate — the project showcases SQL fluency, transactional control, and clean three-tier architecture without leaning on a framework.

## Architecture

```
┌─────────────────────────────────────────────────┐
│  UI Layer (Swing)                                │
│  LoginScreen → MainFrame → role-specific panels  │
└────────────────────────┬────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────┐
│  Service Layer                                   │
│  • Business logic                                │
│  • Role-based access control (Session.role())   │
│  • Multi-DAO atomic transactions                 │
└────────────────────────┬────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────┐
│  DAO Layer                                       │
│  • PreparedStatement only                        │
│  • try-with-resources for every connection       │
│  • Sealed Result types for predictable errors    │
└────────────────────────┬────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────┐
│  MySQL — `travelres` schema (13 normalized tables) │
└─────────────────────────────────────────────────┘
```

### Why three tiers?

- **UI layer** never talks to the database directly — it only invokes services.
- **Service layer** owns transactions and authorization. Every public method that mutates data starts with a `Session.role()` check.
- **DAO layer** is a thin wrapper around `PreparedStatement`. No business logic lives here.

This separation makes the code testable, the access-control story auditable, and the SQL inspectable for a code reviewer.

## Schema

13 normalized tables:

```
Reference data:    Airline, Aircraft, Airport, AirlineAirport (M:N)
Schedule:          Flight
People:            Customer, Employee
Bookings:          Reservation → Ticket → TicketFlight
Customer service:  WaitlistEntry, Notification, Inquiry
```

See [`docs/SCHEMA.md`](docs/SCHEMA.md) for the full design rationale, including the five additions made during development to support the customer-service features (Inquiry table, Notification table, Flight.basePrice, Employee credentials, simplified Reservation/Ticket relationship).

## Quick Start

### Prerequisites

- Java 21 (Temurin or Oracle)
- Apache Maven 3.9+
- MySQL Server 8.0+

### 1. Clone

```bash
git clone https://github.com/<your-username>/travel-reservation-system.git
cd travel-reservation-system
```

### 2. Set up the database

```bash
mysql -u root -p < db/reload-all.sql
```

This drops and recreates the `travelres` database with seed data: 3 airlines, 6 airports, 3 aircraft, 12 flights, plus 4 demo accounts.

### 3. Configure your local MySQL password

Edit `src/main/java/cs336/travel/Db.java`:

```java
public static final String PASSWORD = "your_mysql_root_password";
```

### 4. Build and run

```bash
mvn clean package
java -jar target/travel-1.0-SNAPSHOT.jar
```

The login screen will open. Use one of the seeded accounts:

| Role | Username | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Customer Rep | `rep1` | `rep123` |
| Customer | `alice` | `alice123` |
| Customer | `bob` | `bob123` |

## Project Structure

```
.
├── pom.xml                    # Maven build configuration
├── README.md                  # this file
├── LICENSE
│
├── src/main/java/cs336/travel/
│   ├── App.java               # entry point — opens MainFrame
│   ├── Db.java                # JDBC connection helper
│   ├── Session.java           # current-user holder
│   ├── model/                 # records, enums, sealed result types
│   ├── dao/                   # one DAO per table family
│   ├── service/               # business logic + role checks
│   └── ui/                    # Swing panels and dialogs
│
├── db/
│   ├── schema.sql             # authoritative DDL
│   ├── seed-flights.sql       # demo data
│   └── reload-all.sql         # one-shot reset + reseed
│
└── docs/
    ├── SCHEMA.md              # schema design rationale
    └── SETUP.md               # detailed environment setup notes
```

## Design Decisions

A few choices worth calling out for code reviewers:

**Plain-text passwords** — passwords are stored unhashed because this began as a teaching project where focus was on relational design rather than auth security. A bcrypt migration is straightforward (single column type change + service-layer hash on login/save).

**Class-tier pricing in code** — `Flight.basePrice` stores the economy fare; Business and First multipliers (`× 2.5`, `× 4.0`) live in `PricingService`. A `FlightFare` table would normalize this further but added little for the demo dataset.

**Single-pool capacity** — `Aircraft.seatCapacity` is shared across all classes per flight instance. A `(flightInstance, class) → capacity` table would partition this; not done here to keep the rubric's `INT seatCapacity` field clean.

**FIFO, class-blind waitlist** — when a CONFIRMED ticket cancels, the oldest `WAITING` entry on the same flight/date is promoted regardless of class. Simpler to reason about than per-class FIFOs.

**No connection pool** — single-user demo flow doesn't need pooling. Adding HikariCP is a one-line `pom.xml` change if needed.

Each of these is documented as an explicit decision rather than an oversight.

## Roadmap

Things that would be reasonable extensions:

- [ ] Migrate to bcrypt password hashing
- [ ] Replace Swing with a JavaFX or web frontend (React + REST API)
- [ ] Add JUnit + integration tests with Testcontainers
- [ ] Dockerize MySQL for one-command setup
- [ ] Connection pooling (HikariCP) for multi-user scenarios
- [ ] Per-class capacity model + class-aware waitlist promotion
- [ ] Multi-segment connecting itineraries (current scope is direct flights only)
- [ ] Email/SMS notifications instead of in-app only

## Contributing

Pull requests welcome. For larger changes, open an issue first to discuss the approach.

```bash
git checkout -b feat/your-feature
# make changes
mvn clean package    # ensure it still builds
git commit -m "feat: short description"
git push origin feat/your-feature
# open a PR on GitHub
```

## Authors

- **Youjie Zhang**
- **Kevin Wang**


Originally developed as a database systems coursework project; now maintained as an open-source learning artifact.

## License

MIT — see [LICENSE](LICENSE) for details.
