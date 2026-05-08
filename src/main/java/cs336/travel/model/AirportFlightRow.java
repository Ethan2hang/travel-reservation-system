package cs336.travel.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public record AirportFlightRow(
        String direction,
        String airlineID,
        String flightNumber,
        String otherEndpoint,
        LocalTime departureTime,
        LocalTime arrivalTime,
        String operatingDays,
        String aircraftID,
        BigDecimal basePrice) {
}
