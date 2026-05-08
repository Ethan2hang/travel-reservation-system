package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.ReservationSummary;
import cs336.travel.model.TripType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class ReservationDAO {

    private ReservationDAO() {}

    /**
     * Caller-supplied connection variant for transactional booking.
     * Returns the generated {@code reservationID}. Schema defaults
     * {@code reservationDate=NOW()} and {@code status='CONFIRMED'}.
     */
    public static int insertReservation(Connection c, int customerID, Integer createdByEmployeeID)
            throws SQLException {
        String sql = """
                INSERT INTO Reservation (customerID, createdByEmployeeID)
                VALUES (?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerID);
            if (createdByEmployeeID == null) ps.setNull(2, java.sql.Types.INTEGER);
            else                             ps.setInt(2, createdByEmployeeID);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Reservation insert returned no key");
                return keys.getInt(1);
            }
        }
    }

    /** Standalone variant — opens its own connection. Not used inside a transaction. */
    public static int insertReservation(int customerID, Integer createdByEmployeeID) {
        try (Connection c = Db.getConnection()) {
            return insertReservation(c, customerID, createdByEmployeeID);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "insertReservation failed: customer=" + customerID
                            + " createdBy=" + createdByEmployeeID, e);
        }
    }

    /**
     * Customer's reservation history, bucketed by the earliest segment date.
     * Bucket boundary uses SQL {@code NOW()} so a single source of truth
     * decides past vs. upcoming.
     *
     * @param upcoming {@code true} for {@code earliestDep >= NOW()},
     *                 {@code false} for {@code earliestDep < NOW()}
     */
    public static List<ReservationSummary> listForCustomer(int customerID, boolean upcoming) {
        // Group at (reservation, ticket) level. The current booking flow inserts
        // exactly one Ticket per Reservation, but the schema permits 1+, so a
        // future multi-ticket booking would surface as multiple rows here.
        // Java text blocks strip trailing whitespace, so don't try to splice
        // operator/order keywords into the middle of a """...""" — assemble
        // the predicate-and-order tail as a normal string with explicit spaces.
        // Upcoming hides cancelled rows (they're not "trips" any more); Past
        // shows everything for audit purposes.
        String op    = upcoming ? ">=" : "<";
        String order = upcoming ? "ASC" : "DESC";
        String statusFilter = upcoming ? " AND r.status = 'CONFIRMED' " : " ";
        String sql = """
                SELECT r.reservationID, r.reservationDate, r.status,
                       t.tripType, t.totalFare,
                       MIN(tf.departureDateTime) AS earliestDep,
                       COUNT(tf.segmentOrder)    AS segCount
                FROM Reservation r
                JOIN Ticket       t  ON t.reservationID  = r.reservationID
                JOIN TicketFlight tf ON tf.ticketNumber  = t.ticketNumber
                """
                + "WHERE r.customerID = ? " + statusFilter
                + """
                GROUP BY r.reservationID, r.reservationDate, r.status,
                         t.ticketNumber, t.tripType, t.totalFare
                """
                + "HAVING MIN(tf.departureDateTime) " + op + " NOW() "
                + "ORDER BY MIN(tf.departureDateTime) " + order;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                List<ReservationSummary> out = new ArrayList<>();
                while (rs.next()) out.add(fromSummaryRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "listForCustomer failed: customer=" + customerID
                            + " upcoming=" + upcoming, e);
        }
    }

    private static ReservationSummary fromSummaryRow(ResultSet rs) throws SQLException {
        return new ReservationSummary(
                rs.getInt("reservationID"),
                rs.getTimestamp("reservationDate").toLocalDateTime(),
                TripType.valueOf(rs.getString("tripType")),
                rs.getInt("segCount"),
                rs.getBigDecimal("totalFare"),
                rs.getString("status"),
                rs.getTimestamp("earliestDep").toLocalDateTime());
    }

    /**
     * Owner + status of a reservation, on a caller-supplied connection — used
     * inside the cancel transaction. Empty when no such reservation exists.
     */
    public static java.util.Optional<int[]> findOwnerAndStatus(Connection c, int reservationID)
            throws SQLException {
        String sql = "SELECT customerID, status FROM Reservation WHERE reservationID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                int cust = rs.getInt("customerID");
                int statusCode = "CONFIRMED".equals(rs.getString("status")) ? 1 : 0;
                return java.util.Optional.of(new int[]{cust, statusCode});
            }
        }
    }

    /**
     * Status of a reservation, on a caller-supplied connection. {@code null}
     * status means the reservation doesn't exist. Used by the CR edit flow
     * to refuse edits on cancelled or missing reservations.
     */
    public static String findStatus(Connection c, int reservationID) throws SQLException {
        String sql = "SELECT status FROM Reservation WHERE reservationID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("status") : null;
            }
        }
    }

    /** Flips status to CANCELLED if currently CONFIRMED. Returns rows updated (0 or 1). */
    public static int markCancelled(Connection c, int reservationID) throws SQLException {
        String sql = "UPDATE Reservation SET status = 'CANCELLED' "
                   + "WHERE reservationID = ? AND status = 'CONFIRMED'";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            return ps.executeUpdate();
        }
    }

    /** Header row for the CR edit screen — single reservation, includes customer label. */
    public record EditHeader(
            int reservationID,
            String customerUsername,
            String customerName,
            String status,
            String tripType,
            java.math.BigDecimal totalFare,
            java.time.LocalDateTime bookedOn) {}

    public static java.util.Optional<EditHeader> findHeader(int reservationID) {
        String sql = """
                SELECT r.reservationID, r.reservationDate, r.status,
                       t.tripType, t.totalFare,
                       c.username AS customerUsername, c.name AS customerName
                FROM Reservation r
                JOIN Customer c ON c.customerID    = r.customerID
                JOIN Ticket   t ON t.reservationID = r.reservationID
                WHERE r.reservationID = ?
                LIMIT 1
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return java.util.Optional.empty();
                return java.util.Optional.of(new EditHeader(
                        rs.getInt("reservationID"),
                        rs.getString("customerUsername"),
                        rs.getString("customerName"),
                        rs.getString("status"),
                        rs.getString("tripType"),
                        rs.getBigDecimal("totalFare"),
                        rs.getTimestamp("reservationDate").toLocalDateTime()));
            }
        } catch (SQLException e) {
            throw new RuntimeException("findHeader failed: " + reservationID, e);
        }
    }

    /** CONFIRMED reservations for this customer — used by Admin's pre-delete check. */
    public static int countConfirmedForCustomer(int customerID) {
        String sql = "SELECT COUNT(*) AS cnt FROM Reservation "
                   + "WHERE customerID = ? AND status = 'CONFIRMED'";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countConfirmedForCustomer failed: " + customerID, e);
        }
    }

    /** Reservations created on behalf of someone by this employee. */
    public static int countCreatedByEmployee(int employeeID) {
        String sql = "SELECT COUNT(*) AS cnt FROM Reservation WHERE createdByEmployeeID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, employeeID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countCreatedByEmployee failed: " + employeeID, e);
        }
    }

    /** Confirms a reservation belongs to the given customer — used by service-layer access checks. */
    public static boolean ownedBy(int reservationID, int customerID) {
        String sql = "SELECT 1 FROM Reservation WHERE reservationID = ? AND customerID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            ps.setInt(2, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "ownedBy failed: reservation=" + reservationID + " customer=" + customerID, e);
        }
    }
}
