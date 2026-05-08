package cs336.travel.model;

import java.time.LocalDate;

public record PromotedEntry(
        int waitlistID,
        int customerID,
        String airlineID,
        String flightNumber,
        LocalDate flightDate,
        TravelClass travelClass) {
}
