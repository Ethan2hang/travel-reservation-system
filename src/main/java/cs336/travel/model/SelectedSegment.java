package cs336.travel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One leg picked from a search result. Combines the schedule's {@code TIME}
 * with the instance date into a single {@code LocalDateTime} that maps to
 * {@code TicketFlight.departureDateTime}.
 */
public record SelectedSegment(
        String airlineID,
        String flightNumber,
        LocalDateTime departureDateTime,
        BigDecimal basePrice) {
}
