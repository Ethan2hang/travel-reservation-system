package cs336.travel.model;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Row returned by {@code FlightDAO.searchOneWay} / {@code searchOneWayFlexible}.
 *
 * <p>{@code flightDate} is the *instance* date for this row.
 * {@code duration} is precomputed at the DAO layer with midnight-crossing
 * handled (arrival ≤ departure ⇒ next-day arrival).
 * {@code stops} is always 0 — multi-segment itineraries are out of scope;
 * the rubric still requires the column.
 * {@code displayedPrice} = {@code basePrice} × class multiplier.
 */
public record FlightSearchResult(
        LocalDate flightDate,
        String airlineID,
        String airlineName,
        String flightNumber,
        String departureAirport,
        String arrivalAirport,
        LocalTime departureTime,
        LocalTime arrivalTime,
        Duration duration,
        int stops,
        TravelClass travelClass,
        BigDecimal basePrice,
        BigDecimal displayedPrice) {
}
