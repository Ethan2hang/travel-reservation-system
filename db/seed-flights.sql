-- =====================================================================
-- CS336 Travel Reservation — flight demo data
-- Run AFTER schema.sql:
--   mysql -u root -p travelres < db/seed-flights.sql
-- Idempotent: uses INSERT IGNORE so re-runs are safe. Does not touch
-- Customer / Employee / Reservation rows seeded by schema.sql.
-- =====================================================================

USE travelres;

-- ---------------------------------------------------------------------
-- Airlines
-- ---------------------------------------------------------------------
INSERT IGNORE INTO Airline (airlineID, airlineName) VALUES
    ('AA', 'American Airlines'),
    ('UA', 'United Airlines'),
    ('DL', 'Delta Air Lines');

-- ---------------------------------------------------------------------
-- Airports
-- ---------------------------------------------------------------------
INSERT IGNORE INTO Airport (airportID, name, city, country) VALUES
    ('EWR', 'Newark Liberty Intl',  'Newark',         'USA'),
    ('LGA', 'LaGuardia',             'New York',      'USA'),
    ('JFK', 'John F Kennedy Intl',  'New York',       'USA'),
    ('LAX', 'Los Angeles Intl',     'Los Angeles',    'USA'),
    ('ORD', 'O''Hare Intl',          'Chicago',       'USA'),
    ('SFO', 'San Francisco Intl',   'San Francisco', 'USA');

-- ---------------------------------------------------------------------
-- Aircraft (one per airline, capacity 180)
-- ---------------------------------------------------------------------
INSERT IGNORE INTO Aircraft (aircraftID, airlineID, seatCapacity) VALUES
    ('AA-N101AA', 'AA', 180),
    ('UA-N201UA', 'UA', 180),
    ('DL-N301DL', 'DL', 180);

-- ---------------------------------------------------------------------
-- AirlineAirport — every airline operates at every seeded airport
-- ---------------------------------------------------------------------
INSERT IGNORE INTO AirlineAirport (airlineID, airportID) VALUES
    ('AA','EWR'),('AA','LGA'),('AA','JFK'),('AA','LAX'),('AA','ORD'),('AA','SFO'),
    ('UA','EWR'),('UA','LGA'),('UA','JFK'),('UA','LAX'),('UA','ORD'),('UA','SFO'),
    ('DL','EWR'),('DL','LGA'),('DL','JFK'),('DL','LAX'),('DL','ORD'),('DL','SFO');

-- ---------------------------------------------------------------------
-- Flights — 12 rows, 4 routes × 3 airlines, varied times/prices/days
-- All domestic. Days mix weekdays and weekends so search-by-date filters
-- by Flight.operatingDays meaningfully.
-- ---------------------------------------------------------------------
INSERT IGNORE INTO Flight
    (airlineID, flightNumber, aircraftID, departureAirport, arrivalAirport,
     departureTime, arrivalTime, operatingDays, isDomestic, basePrice) VALUES
    -- EWR <-> LAX
    ('AA', 'AA100', 'AA-N101AA', 'EWR', 'LAX', '06:00:00', '09:30:00', 'MON,WED,FRI',         TRUE, 320.00),
    ('UA', 'UA200', 'UA-N201UA', 'EWR', 'LAX', '14:15:00', '17:45:00', 'TUE,THU,SAT,SUN',     TRUE, 295.50),
    ('DL', 'DL300', 'DL-N301DL', 'LAX', 'EWR', '22:30:00', '06:30:00', 'MON,TUE,WED,THU,FRI', TRUE, 410.00),

    -- JFK <-> SFO
    ('AA', 'AA110', 'AA-N101AA', 'JFK', 'SFO', '07:45:00', '11:30:00', 'TUE,THU,SAT',         TRUE, 380.00),
    ('UA', 'UA210', 'UA-N201UA', 'SFO', 'JFK', '23:00:00', '07:30:00', 'MON,WED,FRI,SUN',     TRUE, 365.00),
    ('DL', 'DL310', 'DL-N301DL', 'JFK', 'SFO', '12:00:00', '15:45:00', 'MON,TUE,WED,THU,FRI', TRUE, 445.00),

    -- LGA <-> ORD
    ('AA', 'AA120', 'AA-N101AA', 'LGA', 'ORD', '08:30:00', '10:15:00', 'MON,TUE,WED,THU,FRI,SAT,SUN', TRUE, 175.00),
    ('UA', 'UA220', 'UA-N201UA', 'LGA', 'ORD', '17:30:00', '19:15:00', 'MON,WED,FRI',         TRUE, 199.00),
    ('DL', 'DL320', 'DL-N301DL', 'ORD', 'LGA', '06:45:00', '10:00:00', 'SAT,SUN',             TRUE, 210.00),

    -- EWR <-> ORD
    ('AA', 'AA130', 'AA-N101AA', 'EWR', 'ORD', '11:00:00', '12:45:00', 'TUE,THU',             TRUE, 155.00),
    ('UA', 'UA230', 'UA-N201UA', 'ORD', 'EWR', '19:00:00', '22:00:00', 'MON,TUE,WED,THU,FRI', TRUE, 180.00),
    ('DL', 'DL330', 'DL-N301DL', 'EWR', 'ORD', '15:30:00', '17:15:00', 'FRI,SAT,SUN',         TRUE, 165.00);
