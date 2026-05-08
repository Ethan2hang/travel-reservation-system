package cs336.travel.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * One ticket-level row underlying a revenue detail view. Fields are generic
 * enough to render in all three "Revenue: by …" mode tables; the panel
 * picks which columns to show.
 */
public record RevenueDetailRow(
        int reservationID,
        String customerUsername,
        String customerName,
        LocalDateTime purchaseDateTime,
        String airlineID,
        String flightNumber,
        TravelClass travelClass,
        BigDecimal lineTotal) {
}
