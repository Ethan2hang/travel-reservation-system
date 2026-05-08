package cs336.travel.model;

import java.time.LocalDateTime;

public record SegmentDetail(
        int segmentOrder,
        String airlineID,
        String flightNumber,
        String fromAirport,
        String toAirport,
        LocalDateTime departureDateTime,
        LocalDateTime arrivalDateTime,
        TravelClass travelClass,
        String seatNumber) {
}
