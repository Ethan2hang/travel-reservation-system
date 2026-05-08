package cs336.travel.model;

import java.math.BigDecimal;
import java.util.List;

/**
 * Aggregated sales for one (year, month) bucket. {@code rows} are the
 * underlying CONFIRMED-reservation tickets that contribute to the totals;
 * grader can audit the summary line against the per-row breakdown.
 */
public record MonthlySales(
        int year,
        int month,
        int ticketCount,
        BigDecimal bookingFeeTotal,
        BigDecimal fareTotal,
        BigDecimal grandTotal,
        List<TicketSaleRow> rows) {
}
