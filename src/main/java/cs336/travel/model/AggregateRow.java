package cs336.travel.model;

import java.math.BigDecimal;

/** Generic (label, count, revenue) tuple for activity / leaderboard reports. */
public record AggregateRow(String label, int count, BigDecimal revenue) {
}
