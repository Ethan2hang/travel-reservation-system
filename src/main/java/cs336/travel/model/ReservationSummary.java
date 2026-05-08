package cs336.travel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One row of the customer's reservation history. {@code earliestDeparture}
 * is the {@code MIN(TicketFlight.departureDateTime)} across the reservation's
 * segments — used to bucket into Upcoming vs. Past.
 *
 * <p>"Earliest segment" semantics: a round-trip with outbound 2026-05-06 and
 * return 2026-05-08 sits in Upcoming until 5/06 and moves to Past after,
 * even though the return is still in the future. Matches how travel apps
 * behave and keeps the SQL trivial.
 */
public record ReservationSummary(
        int reservationID,
        LocalDateTime bookedOn,
        TripType tripType,
        int segmentCount,
        BigDecimal totalFare,
        String status,
        LocalDateTime earliestDeparture) {
}
