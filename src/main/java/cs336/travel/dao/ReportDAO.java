package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.AggregateRow;
import cs336.travel.model.MonthlySales;
import cs336.travel.model.ReservationLookupRow;
import cs336.travel.model.RevenueDetail;
import cs336.travel.model.RevenueDetailRow;
import cs336.travel.model.TicketSaleRow;
import cs336.travel.model.TopCustomerRow;
import cs336.travel.model.TravelClass;
import cs336.travel.model.TripType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public final class ReportDAO {

    private ReportDAO() {}

    /**
     * Tickets purchased in the given (year, month) whose owning Reservation
     * is still {@code CONFIRMED}. Cancelled sales don't generate revenue in
     * our model, so they're excluded from totals.
     *
     * <p>One round-trip; aggregates summed in Java. Row counts are bounded by
     * the school project's dataset size — sequential scan is acceptable.
     */
    public static MonthlySales salesForMonth(int year, int month) {
        String sql = """
                SELECT t.ticketNumber, t.reservationID, t.tripType,
                       t.purchaseDateTime, t.bookingFee, t.totalFare,
                       c.username AS customerUsername, c.name AS customerName
                FROM Ticket      t
                JOIN Reservation r ON r.reservationID = t.reservationID
                JOIN Customer    c ON c.customerID    = r.customerID
                WHERE r.status = 'CONFIRMED'
                  AND YEAR(t.purchaseDateTime)  = ?
                  AND MONTH(t.purchaseDateTime) = ?
                ORDER BY t.purchaseDateTime ASC, t.ticketNumber ASC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, year);
            ps.setInt(2, month);
            try (ResultSet rs = ps.executeQuery()) {
                List<TicketSaleRow> rows = new ArrayList<>();
                BigDecimal feeTotal  = BigDecimal.ZERO;
                BigDecimal fareTotal = BigDecimal.ZERO;
                while (rs.next()) {
                    TicketSaleRow row = fromRow(rs);
                    rows.add(row);
                    feeTotal  = feeTotal.add(row.bookingFee());
                    fareTotal = fareTotal.add(row.totalFare());
                }
                return new MonthlySales(
                        year, month, rows.size(),
                        feeTotal, fareTotal, feeTotal.add(fareTotal),
                        rows);
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "salesForMonth failed: " + year + "-" + month, e);
        }
    }

    /**
     * Reservations whose tickets include at least one segment on the given
     * flight (any date). Includes both CONFIRMED and CANCELLED — the admin
     * lookup is meant to show the full audit picture.
     */
    public static List<ReservationLookupRow> reservationsForFlight(
            String airlineID, String flightNumber) {
        // SUM(t.totalFare) handles the (currently 1:1) Reservation→Ticket link
        // and would still aggregate correctly if a Reservation gained 2+ Tickets.
        String sql = """
                SELECT r.reservationID, r.reservationDate, r.status,
                       c.username AS customerUsername, c.name AS customerName,
                       MIN(t.tripType)             AS tripType,
                       COALESCE(SUM(t.totalFare),0) AS totalFare
                FROM Reservation r
                JOIN Customer    c ON c.customerID    = r.customerID
                JOIN Ticket      t ON t.reservationID = r.reservationID
                WHERE EXISTS (
                    SELECT 1
                    FROM TicketFlight tf
                    WHERE tf.ticketNumber = t.ticketNumber
                      AND tf.airlineID    = ?
                      AND tf.flightNumber = ?
                )
                GROUP BY r.reservationID, r.reservationDate, r.status,
                         c.username, c.name
                ORDER BY r.reservationDate DESC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReservationLookupRow> out = new ArrayList<>();
                while (rs.next()) out.add(fromLookupRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "reservationsForFlight failed: " + airlineID + flightNumber, e);
        }
    }

    /**
     * Reservations whose customer name matches a fuzzy LIKE pattern. The query
     * argument is wrapped in {@code %...%} and compared case-insensitively.
     */
    public static List<ReservationLookupRow> reservationsForCustomerName(String query) {
        String sql = """
                SELECT r.reservationID, r.reservationDate, r.status,
                       c.username AS customerUsername, c.name AS customerName,
                       MIN(t.tripType)             AS tripType,
                       COALESCE(SUM(t.totalFare),0) AS totalFare
                FROM Reservation r
                JOIN Customer    c ON c.customerID    = r.customerID
                LEFT JOIN Ticket t ON t.reservationID = r.reservationID
                WHERE LOWER(c.name) LIKE LOWER(?)
                GROUP BY r.reservationID, r.reservationDate, r.status,
                         c.username, c.name
                ORDER BY c.name ASC, r.reservationDate DESC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + (query == null ? "" : query.trim()) + "%");
            try (ResultSet rs = ps.executeQuery()) {
                List<ReservationLookupRow> out = new ArrayList<>();
                while (rs.next()) out.add(fromLookupRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "reservationsForCustomerName failed: " + query, e);
        }
    }

    private static final NumberFormat MONEY = NumberFormat.getCurrencyInstance(Locale.US);

    /** Tickets whose tickets touch (airlineID, flightNumber); status=CONFIRMED. */
    public static RevenueDetail revenueByFlight(String airlineID, String flightNumber) {
        // DISTINCT to avoid double-counting a ticket that has multiple
        // segments on the same flight (rare in this dataset, but safe).
        String sql = """
                SELECT DISTINCT t.ticketNumber, t.reservationID, t.purchaseDateTime,
                       t.bookingFee, t.totalFare,
                       c.username AS customerUsername, c.name AS customerName,
                       tf.airlineID, tf.flightNumber, tf.class AS class
                FROM Ticket       t
                JOIN Reservation  r  ON r.reservationID = t.reservationID
                JOIN Customer     c  ON c.customerID    = r.customerID
                JOIN TicketFlight tf ON tf.ticketNumber = t.ticketNumber
                WHERE r.status = 'CONFIRMED'
                  AND tf.airlineID    = ?
                  AND tf.flightNumber = ?
                ORDER BY t.purchaseDateTime DESC
                """;
        List<RevenueDetailRow> rows = runDetail(sql, ps -> {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
        }, "revenueByFlight " + airlineID + flightNumber);
        BigDecimal total = sumLineTotals(rows);
        String summary = "Revenue for " + airlineID + " " + flightNumber + ": "
                + MONEY.format(total) + " (" + rows.size() + " ticket"
                + (rows.size() == 1 ? "" : "s") + " sold)";
        return new RevenueDetail(summary, total, rows.size(), rows);
    }

    /** All tickets touching any flight where Flight.airlineID = ?. */
    public static RevenueDetail revenueByAirline(String airlineID) {
        String sql = """
                SELECT DISTINCT t.ticketNumber, t.reservationID, t.purchaseDateTime,
                       t.bookingFee, t.totalFare,
                       c.username AS customerUsername, c.name AS customerName,
                       tf.airlineID, tf.flightNumber, tf.class AS class
                FROM Ticket       t
                JOIN Reservation  r  ON r.reservationID = t.reservationID
                JOIN Customer     c  ON c.customerID    = r.customerID
                JOIN TicketFlight tf ON tf.ticketNumber = t.ticketNumber
                WHERE r.status = 'CONFIRMED'
                  AND tf.airlineID = ?
                ORDER BY t.purchaseDateTime DESC
                """;
        List<RevenueDetailRow> rows = runDetail(sql,
                ps -> ps.setString(1, airlineID),
                "revenueByAirline " + airlineID);
        BigDecimal total = sumLineTotals(rows);
        String summary = "Revenue for " + airlineID + ": "
                + MONEY.format(total) + " (" + rows.size() + " ticket"
                + (rows.size() == 1 ? "" : "s") + " sold)";
        return new RevenueDetail(summary, total, rows.size(), rows);
    }

    /** Tickets booked by customers whose name fuzzy-matches the fragment. */
    public static RevenueDetail revenueByCustomer(String nameFragment) {
        String sql = """
                SELECT t.ticketNumber, t.reservationID, t.purchaseDateTime,
                       t.bookingFee, t.totalFare,
                       c.username AS customerUsername, c.name AS customerName,
                       NULL AS airlineID, NULL AS flightNumber,
                       (SELECT MIN(tf.class) FROM TicketFlight tf
                        WHERE tf.ticketNumber = t.ticketNumber) AS class
                FROM Ticket       t
                JOIN Reservation  r  ON r.reservationID = t.reservationID
                JOIN Customer     c  ON c.customerID    = r.customerID
                WHERE r.status = 'CONFIRMED'
                  AND LOWER(c.name) LIKE LOWER(?)
                ORDER BY c.name ASC, t.purchaseDateTime DESC
                """;
        String like = "%" + (nameFragment == null ? "" : nameFragment.trim()) + "%";
        List<RevenueDetailRow> rows = runDetail(sql,
                ps -> ps.setString(1, like),
                "revenueByCustomer " + nameFragment);
        BigDecimal total = sumLineTotals(rows);
        String summary = "Revenue from customers matching \""
                + (nameFragment == null ? "" : nameFragment.trim()) + "\": "
                + MONEY.format(total) + " (" + rows.size() + " ticket"
                + (rows.size() == 1 ? "" : "s") + ")";
        return new RevenueDetail(summary, total, rows.size(), rows);
    }

    public static Optional<TopCustomerRow> topCustomerByRevenue() {
        String sql = """
                SELECT c.customerID, c.username, c.name,
                       COUNT(t.ticketNumber) AS tix,
                       COALESCE(SUM(t.totalFare + t.bookingFee), 0) AS rev
                FROM Customer    c
                JOIN Reservation r ON r.customerID    = c.customerID
                JOIN Ticket      t ON t.reservationID = r.reservationID
                WHERE r.status = 'CONFIRMED'
                GROUP BY c.customerID, c.username, c.name
                ORDER BY rev DESC, tix DESC
                LIMIT 1
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return Optional.empty();
            return Optional.of(new TopCustomerRow(
                    rs.getInt("customerID"),
                    rs.getString("username"),
                    rs.getString("name"),
                    rs.getInt("tix"),
                    rs.getBigDecimal("rev")));
        } catch (SQLException e) {
            throw new RuntimeException("topCustomerByRevenue failed", e);
        }
    }

    /** Top N flights by COUNT(TicketFlight) on CONFIRMED reservations. */
    public static List<AggregateRow> mostActiveFlights(int limit) {
        String sql = """
                SELECT tf.airlineID, tf.flightNumber,
                       COUNT(*) AS tix,
                       COALESCE(SUM(t.totalFare + t.bookingFee), 0) AS rev
                FROM TicketFlight tf
                JOIN Ticket       t ON t.ticketNumber  = tf.ticketNumber
                JOIN Reservation  r ON r.reservationID = t.reservationID
                WHERE r.status = 'CONFIRMED'
                GROUP BY tf.airlineID, tf.flightNumber
                ORDER BY tix DESC, rev DESC
                LIMIT ?
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AggregateRow> out = new ArrayList<>();
                while (rs.next()) {
                    String label = rs.getString("airlineID") + " " + rs.getString("flightNumber");
                    out.add(new AggregateRow(
                            label,
                            rs.getInt("tix"),
                            rs.getBigDecimal("rev")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("mostActiveFlights failed: limit=" + limit, e);
        }
    }

    @FunctionalInterface
    private interface PsBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private static List<RevenueDetailRow> runDetail(String sql, PsBinder binder, String ctx) {
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                List<RevenueDetailRow> out = new ArrayList<>();
                while (rs.next()) out.add(fromRevenueDetailRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(ctx + " failed", e);
        }
    }

    private static RevenueDetailRow fromRevenueDetailRow(ResultSet rs) throws SQLException {
        BigDecimal fee  = rs.getBigDecimal("bookingFee");
        BigDecimal fare = rs.getBigDecimal("totalFare");
        String cls = rs.getString("class");
        return new RevenueDetailRow(
                rs.getInt("reservationID"),
                rs.getString("customerUsername"),
                rs.getString("customerName"),
                rs.getTimestamp("purchaseDateTime").toLocalDateTime(),
                rs.getString("airlineID"),
                rs.getString("flightNumber"),
                cls == null ? null : TravelClass.valueOf(cls),
                fee.add(fare));
    }

    private static BigDecimal sumLineTotals(List<RevenueDetailRow> rows) {
        BigDecimal s = BigDecimal.ZERO;
        for (RevenueDetailRow r : rows) s = s.add(r.lineTotal());
        return s;
    }

    private static ReservationLookupRow fromLookupRow(ResultSet rs) throws SQLException {
        String tripType = rs.getString("tripType");
        return new ReservationLookupRow(
                rs.getInt("reservationID"),
                rs.getString("customerUsername"),
                rs.getString("customerName"),
                rs.getString("status"),
                rs.getTimestamp("reservationDate").toLocalDateTime(),
                tripType == null ? null : TripType.valueOf(tripType),
                rs.getBigDecimal("totalFare"));
    }

    private static TicketSaleRow fromRow(ResultSet rs) throws SQLException {
        BigDecimal fee  = rs.getBigDecimal("bookingFee");
        BigDecimal fare = rs.getBigDecimal("totalFare");
        return new TicketSaleRow(
                rs.getLong("ticketNumber"),
                rs.getInt("reservationID"),
                rs.getString("customerUsername"),
                rs.getString("customerName"),
                TripType.valueOf(rs.getString("tripType")),
                rs.getTimestamp("purchaseDateTime").toLocalDateTime(),
                fee,
                fare,
                fee.add(fare));
    }
}
