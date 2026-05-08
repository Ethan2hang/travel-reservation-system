package cs336.travel.model;

import java.math.BigDecimal;

public record TopCustomerRow(
        int customerID,
        String username,
        String name,
        int ticketCount,
        BigDecimal totalRevenue) {
}
