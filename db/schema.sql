-- =====================================================================
-- CS336 Travel Reservation System — MySQL schema
-- Version 2: incorporates 5 fixes flagged after Deliverable 1 review
-- Run with: mysql -u root -p < db/schema.sql
-- =====================================================================

DROP DATABASE IF EXISTS travelres;
CREATE DATABASE travelres CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travelres;

-- ---------------------------------------------------------------------
-- Reference data
-- ---------------------------------------------------------------------

CREATE TABLE Airline (
    airlineID    CHAR(2)        PRIMARY KEY,           -- e.g. 'AA', 'UA'
    airlineName  VARCHAR(100)   NOT NULL UNIQUE
) ENGINE=InnoDB;

CREATE TABLE Airport (
    airportID    CHAR(3)        PRIMARY KEY,           -- e.g. 'EWR', 'LGA'
    name         VARCHAR(150)   NOT NULL,
    city         VARCHAR(100)   NOT NULL,
    country      VARCHAR(100)   NOT NULL
) ENGINE=InnoDB;

CREATE TABLE Aircraft (
    aircraftID    VARCHAR(20)   PRIMARY KEY,           -- e.g. 'N12345' tail number
    airlineID     CHAR(2)       NOT NULL,
    seatCapacity  INT           NOT NULL CHECK (seatCapacity > 0),
    CONSTRAINT fk_aircraft_airline
        FOREIGN KEY (airlineID) REFERENCES Airline(airlineID)
) ENGINE=InnoDB;

-- M:N — which airports an airline operates at
CREATE TABLE AirlineAirport (
    airlineID  CHAR(2),
    airportID  CHAR(3),
    PRIMARY KEY (airlineID, airportID),
    CONSTRAINT fk_aa_airline FOREIGN KEY (airlineID) REFERENCES Airline(airlineID),
    CONSTRAINT fk_aa_airport FOREIGN KEY (airportID) REFERENCES Airport(airportID)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Flights (recurring schedule, not specific instances)
-- ---------------------------------------------------------------------

CREATE TABLE Flight (
    airlineID         CHAR(2)        NOT NULL,
    flightNumber      VARCHAR(10)    NOT NULL,         -- unique within airline only
    aircraftID        VARCHAR(20)    NOT NULL,
    departureAirport  CHAR(3)        NOT NULL,
    arrivalAirport    CHAR(3)        NOT NULL,
    departureTime     TIME           NOT NULL,         -- scheduled wall-clock
    arrivalTime       TIME           NOT NULL,
    operatingDays     VARCHAR(20)    NOT NULL,         -- comma-list: 'MON,WED,FRI'
    isDomestic        BOOLEAN        NOT NULL,
    basePrice         DECIMAL(10,2)  NOT NULL,         -- FIX 2: economy base price
    PRIMARY KEY (airlineID, flightNumber),
    CONSTRAINT fk_flight_airline   FOREIGN KEY (airlineID)        REFERENCES Airline(airlineID),
    CONSTRAINT fk_flight_aircraft  FOREIGN KEY (aircraftID)       REFERENCES Aircraft(aircraftID),
    CONSTRAINT fk_flight_dep       FOREIGN KEY (departureAirport) REFERENCES Airport(airportID),
    CONSTRAINT fk_flight_arr       FOREIGN KEY (arrivalAirport)   REFERENCES Airport(airportID),
    CONSTRAINT chk_flight_endpoints CHECK (departureAirport <> arrivalAirport)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- People
-- ---------------------------------------------------------------------

CREATE TABLE Customer (
    customerID  INT             AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,              -- plain ok for class project
    name        VARCHAR(100)    NOT NULL,
    email       VARCHAR(150)    NOT NULL UNIQUE,
    phone       VARCHAR(20)
) ENGINE=InnoDB;

-- FIX 3: Employee gains username/password so admin and CR can log in.
CREATE TABLE Employee (
    employeeID  INT             AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)     NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    role        ENUM('ADMIN','CUSTOMER_REP') NOT NULL
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Reservations & tickets
-- FIX 5: Reservation is the *transaction wrapper*. A Reservation contains
-- 1+ Tickets; a Ticket has 1+ TicketFlight segments. The redundant
-- "Customer purchases Ticket" relation is dropped — derivable through
-- Reservation.customerID.
-- ---------------------------------------------------------------------

CREATE TABLE Reservation (
    reservationID         INT          AUTO_INCREMENT PRIMARY KEY,
    customerID            INT          NOT NULL,
    createdByEmployeeID   INT          NULL,            -- not null when CR booked on behalf
    reservationDate       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status                ENUM('CONFIRMED','CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    CONSTRAINT fk_res_cust FOREIGN KEY (customerID)          REFERENCES Customer(customerID),
    CONSTRAINT fk_res_emp  FOREIGN KEY (createdByEmployeeID) REFERENCES Employee(employeeID)
) ENGINE=InnoDB;

CREATE TABLE Ticket (
    ticketNumber       BIGINT          AUTO_INCREMENT PRIMARY KEY,
    reservationID      INT             NOT NULL,
    tripType           ENUM('ONE_WAY','ROUND_TRIP') NOT NULL,
    totalFare          DECIMAL(10,2)   NOT NULL,
    bookingFee         DECIMAL(10,2)   NOT NULL,
    purchaseDateTime   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_res FOREIGN KEY (reservationID) REFERENCES Reservation(reservationID)
) ENGINE=InnoDB;

CREATE TABLE TicketFlight (
    ticketNumber        BIGINT      NOT NULL,
    segmentOrder        INT         NOT NULL,           -- 1, 2, 3 ...
    airlineID           CHAR(2)     NOT NULL,
    flightNumber        VARCHAR(10) NOT NULL,
    departureDateTime   DATETIME    NOT NULL,           -- specific instance, not just TIME
    seatNumber          VARCHAR(5)  NOT NULL,
    class               ENUM('ECONOMY','BUSINESS','FIRST') NOT NULL,
    specialMeal         VARCHAR(50),
    PRIMARY KEY (ticketNumber, segmentOrder),
    CONSTRAINT fk_tf_ticket FOREIGN KEY (ticketNumber) REFERENCES Ticket(ticketNumber)
        ON DELETE CASCADE,
    CONSTRAINT fk_tf_flight FOREIGN KEY (airlineID, flightNumber)
        REFERENCES Flight(airlineID, flightNumber)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Waitlist
-- ---------------------------------------------------------------------

CREATE TABLE WaitlistEntry (
    waitlistID         INT          AUTO_INCREMENT PRIMARY KEY,
    customerID         INT          NOT NULL,
    airlineID          CHAR(2)      NOT NULL,
    flightNumber       VARCHAR(10)  NOT NULL,
    flightDate         DATE         NOT NULL,
    class              ENUM('ECONOMY','BUSINESS','FIRST') NOT NULL,
    requestDateTime    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status             ENUM('WAITING','PROMOTED','EXPIRED') NOT NULL DEFAULT 'WAITING',
    CONSTRAINT fk_wl_cust   FOREIGN KEY (customerID)              REFERENCES Customer(customerID),
    CONSTRAINT fk_wl_flight FOREIGN KEY (airlineID, flightNumber) REFERENCES Flight(airlineID, flightNumber)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Inquiry — FIX 1: customer ↔ CR Q&A
-- ---------------------------------------------------------------------

CREATE TABLE Inquiry (
    inquiryID    INT           AUTO_INCREMENT PRIMARY KEY,
    customerID   INT           NOT NULL,
    question     TEXT          NOT NULL,
    postedAt     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answeredBy   INT           NULL,
    answer       TEXT          NULL,
    answeredAt   DATETIME      NULL,
    status       ENUM('OPEN','ANSWERED') NOT NULL DEFAULT 'OPEN',
    CONSTRAINT fk_inq_cust FOREIGN KEY (customerID) REFERENCES Customer(customerID),
    CONSTRAINT fk_inq_emp  FOREIGN KEY (answeredBy) REFERENCES Employee(employeeID)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Notification — FIX 4: waitlist alert mechanism
-- Insert a row when a seat opens up; customer's screen polls unread.
-- ---------------------------------------------------------------------

CREATE TABLE Notification (
    notificationID  INT           AUTO_INCREMENT PRIMARY KEY,
    customerID      INT           NOT NULL,
    message         VARCHAR(500)  NOT NULL,
    createdAt       DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    readAt          DATETIME      NULL,
    CONSTRAINT fk_notif_cust FOREIGN KEY (customerID) REFERENCES Customer(customerID)
) ENGINE=InnoDB;

-- ---------------------------------------------------------------------
-- Indexes for the queries on the checklist
-- ---------------------------------------------------------------------

CREATE INDEX idx_flight_route       ON Flight(departureAirport, arrivalAirport);
CREATE INDEX idx_tf_flight_date     ON TicketFlight(airlineID, flightNumber, departureDateTime);
CREATE INDEX idx_res_customer_date  ON Reservation(customerID, reservationDate);
CREATE INDEX idx_wl_flight_status   ON WaitlistEntry(airlineID, flightNumber, flightDate, status);
CREATE INDEX idx_notif_unread       ON Notification(customerID, readAt);

-- ---------------------------------------------------------------------
-- Bootstrap admin so the first login is possible
-- ---------------------------------------------------------------------

INSERT INTO Employee (username, password, name, role)
VALUES ('admin', 'admin123', 'System Administrator', 'ADMIN'),
       ('rep1',  'rep123',   'Customer Rep One',     'CUSTOMER_REP');

INSERT INTO Customer (username, password, name, email, phone)
VALUES ('alice', 'alice123', 'Alice Smith', 'alice@example.com', '555-0100'),
       ('bob',   'bob123',   'Bob Jones',   'bob@example.com',   '555-0101');
