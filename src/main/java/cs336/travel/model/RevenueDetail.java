package cs336.travel.model;

import java.math.BigDecimal;
import java.util.List;

public record RevenueDetail(
        String summaryText,
        BigDecimal totalRevenue,
        int ticketCount,
        List<RevenueDetailRow> rows) {
}
