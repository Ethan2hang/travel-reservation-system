package cs336.travel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketSaleRow(
        long ticketNumber,
        int reservationID,
        String customerUsername,
        String customerName,
        TripType tripType,
        LocalDateTime purchaseDateTime,
        BigDecimal bookingFee,
        BigDecimal totalFare,
        BigDecimal lineTotal) {
}
