# Schema Reference

The authoritative DDL is `db/schema.sql`. This doc explains the *why*.

## What changed from Deliverable 1

Five gaps in the submitted ER diagram were closed before any Java code is written. Three are required by the grading checklist; two are normalization cleanups.

| # | Fix | Reason |
|---|---|---|
| 1 | Added `Inquiry` table | Checklist requires "post questions to CR" (Customer §2) and "Reply to user's questions" (CR §3). Original ER didn't model this. |
| 2 | Added `Flight.basePrice` | Customers must filter/sort by price (Customer §1). Price could not be derived without a base. Class price = `basePrice × {1.0, 2.5, 4.0}` for economy/business/first. |
| 3 | Added `Employee.username`, `Employee.password` | Admin and CR must log in (Spec §3 access control). Original Employee had no credentials. |
| 4 | Added `Notification` table | Checklist requires "send an alert to customers in the waiting list" (Customer §2). Implemented as inserted-row + customer-side poll. |
| 5 | Simplified Reservation/Ticket relationship | Original had both `Customer purchases Ticket` and `Customer makes Reservation includes Ticket` — redundant. Reservation is now the transaction wrapper; Ticket is what was bought; relationship to Customer goes through Reservation. |

## Tables

### Reference data

- **Airline** `(airlineID PK, airlineName)` — 2-letter code per spec ("AA", "UA").
- **Airport** `(airportID PK, name, city, country)` — 3-letter code ("EWR", "LGA").
- **Aircraft** `(aircraftID PK, airlineID FK, seatCapacity)` — every aircraft belongs to one airline.
- **AirlineAirport** `(airlineID, airportID)` PK both — M:N "operates at".

### Schedule

- **Flight** `(airlineID, flightNumber)` PK both — `flightNumber` unique only within an airline.
  - `aircraftID` FK Aircraft (the ship that flies it)
  - `departureAirport`, `arrivalAirport` — both FK Airport, must differ (CHECK)
  - `departureTime`, `arrivalTime` — scheduled `TIME` (wall-clock; date is per booking)
  - `operatingDays` — comma list `"MON,WED,FRI"`
  - `isDomestic` — `BOOLEAN` per spec
  - `basePrice` — economy class; multiply for higher classes (see Fix 2)

### People

- **Customer** `(customerID PK, username, password, name, email, phone)`
- **Employee** `(employeeID PK, username, password, name, role)` — `role ∈ {ADMIN, CUSTOMER_REP}`

### Booking

- **Reservation** `(reservationID PK, customerID FK, createdByEmployeeID FK NULL, reservationDate, status)`
  - `createdByEmployeeID` is non-null when a CR booked on behalf of a customer.
  - `status ∈ {CONFIRMED, CANCELLED}`
- **Ticket** `(ticketNumber PK, reservationID FK, tripType, totalFare, bookingFee, purchaseDateTime)`
  - `tripType ∈ {ONE_WAY, ROUND_TRIP}`
  - `bookingFee` is the company's revenue per spec.
- **TicketFlight** `((ticketNumber, segmentOrder) PK, airlineID, flightNumber, departureDateTime, seatNumber, class, specialMeal)`
  - One row per flight segment (round-trip with one stop = 4 rows).
  - `departureDateTime` is a `DATETIME` so we can distinguish Mon-Mar-3 from Mon-Mar-10 of the same scheduled flight.
  - `class ∈ {ECONOMY, BUSINESS, FIRST}` — drives change/cancel policy and price multiplier.

### Customer service

- **WaitlistEntry** `(waitlistID PK, customerID FK, airlineID+flightNumber FK Flight, flightDate, class, requestDateTime, status)`
  - `status ∈ {WAITING, PROMOTED, EXPIRED}`
  - When a CONFIRMED ticket on this flight/date is cancelled, set the oldest WAITING entry to PROMOTED and insert a Notification row.
- **Inquiry** `(inquiryID PK, customerID FK, question, postedAt, answeredBy FK Employee NULL, answer NULL, answeredAt NULL, status)`
- **Notification** `(notificationID PK, customerID FK, message, createdAt, readAt NULL)`
  - Customer-facing alert stream. Polled by the customer's main screen on focus.

## Cardinality summary

```
Airline 1───* Aircraft
Airline *───* Airport          (via AirlineAirport)
Airline 1───* Flight
Aircraft 1──* Flight
Airport 1───* Flight (depart)
Airport 1───* Flight (arrive)

Customer 1──* Reservation
Employee 0──* Reservation       (when CR booked on behalf)
Reservation 1──* Ticket
Ticket 1───* TicketFlight
Flight 1───* TicketFlight       (one flight, many bookings)

Customer 1──* WaitlistEntry
Flight 1───* WaitlistEntry

Customer 1──* Inquiry
Employee 0──* Inquiry            (the answerer)

Customer 1──* Notification
```

## Constraints worth noting

- `Flight.departureAirport <> arrivalAirport` (CHECK).
- `Aircraft.seatCapacity > 0` (CHECK).
- `TicketFlight.ticketNumber` deletes cascade (delete a Ticket, its segments go).
- Unique: `Customer.username`, `Customer.email`, `Employee.username`, `Airline.airlineName`.

## Bootstrap data

Schema seeds one row only: an admin (`admin` / `admin123`). No further seed data — write your own loader script in Java once DAOs exist (see ROADMAP §Day 1).

## Open questions to ask the group

These are deliberate simplifications. Resolve before final submission if anyone disagrees:

1. **Class-price multiplier in code, not DB.** Fixed multipliers `{1.0, 2.5, 4.0}` live in `PricingService`. Easier to demo than a `FlightFare` table; less flexible.
2. **No notification on flight cancellation by airline.** Spec only requires waitlist alerts. If you want to alert all confirmed passengers when a flight is cancelled, the same Notification table works — just add the producer.
3. **Plain-text passwords.** Acceptable for class; flag as a known weakness in your final report.
