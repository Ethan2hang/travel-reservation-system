package cs336.travel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ReservationLookupRow(
        int reservationID,
        String customerUsername,
        String customerName,
        String status,
        LocalDateTime bookedOn,
        TripType tripType,
        BigDecimal totalFare) {
}
