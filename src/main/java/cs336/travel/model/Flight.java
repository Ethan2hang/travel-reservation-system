package cs336.travel.model;

import java.math.BigDecimal;
import java.time.LocalTime;

public record Flight(
        String airlineID,
        String flightNumber,
        String aircraftID,
        String departureAirport,
        String arrivalAirport,
        LocalTime departureTime,
        LocalTime arrivalTime,
        String operatingDays,
        boolean isDomestic,
        BigDecimal basePrice) {
}
