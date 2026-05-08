package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.SegmentDetail;
import cs336.travel.model.TravelClass;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public final class TicketFlightDAO {

    private TicketFlightDAO() {}

    public static void insertSegment(Connection c,
                                     long ticketNumber,
                                     int segmentOrder,
                                     String airlineID,
                                     String flightNumber,
                                     LocalDateTime departureDateTime,
                                     String seatNumber,
                                     TravelClass cls) throws SQLException {
        String sql = """
                INSERT INTO TicketFlight
                  (ticketNumber, segmentOrder, airlineID, flightNumber,
                   departureDateTime, seatNumber, class)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, ticketNumber);
            ps.setInt(2, segmentOrder);
            ps.setString(3, airlineID);
            ps.setString(4, flightNumber);
            ps.setTimestamp(5, Timestamp.valueOf(departureDateTime));
            ps.setString(6, seatNumber);
            ps.setString(7, cls.name());
            ps.executeUpdate();
        }
    }

    public static void insertSegment(long ticketNumber, int segmentOrder,
                                     String airlineID, String flightNumber,
                                     LocalDateTime departureDateTime,
                                     String seatNumber, TravelClass cls) {
        try (Connection c = Db.getConnection()) {
            insertSegment(c, ticketNumber, segmentOrder, airlineID, flightNumber,
                    departureDateTime, seatNumber, cls);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "insertSegment failed: ticket=" + ticketNumber
                            + " seg=" + segmentOrder + " " + airlineID + flightNumber, e);
        }
    }

    /** Standalone variant — opens its own connection. */
    public static int countBookedSeats(String airlineID, String flightNumber,
                                       LocalDateTime departureDateTime) {
        try (Connection c = Db.getConnection()) {
            return countBookedSeats(c, airlineID, flightNumber, departureDateTime);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "countBookedSeats failed: " + airlineID + " " + flightNumber
                            + " " + departureDateTime, e);
        }
    }

    /**
     * Booked seats on a specific flight instance — used for capacity check.
     * Filters out segments whose reservation is CANCELLED so a freed seat
     * is reflected immediately, and waitlist promotion / re-booking see
     * an honest count.
     */
    public static int countBookedSeats(Connection c, String airlineID, String flightNumber,
                                       LocalDateTime departureDateTime) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS cnt
                FROM TicketFlight tf
                JOIN Ticket      t ON t.ticketNumber  = tf.ticketNumber
                JOIN Reservation r ON r.reservationID = t.reservationID
                WHERE tf.airlineID = ? AND tf.flightNumber = ? AND tf.departureDateTime = ?
                  AND r.status = 'CONFIRMED'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            ps.setTimestamp(3, Timestamp.valueOf(departureDateTime));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        }
    }

    /**
     * Returns the largest existing seatNumber on this flight instance, parsed
     * as an integer; 0 if none. Seats are auto-assigned as "1","2","3"...
     * (no seat-picker UI in this feature — see BookingService).
     */
    public static int maxSeatNumber(Connection c, String airlineID, String flightNumber,
                                    LocalDateTime departureDateTime) throws SQLException {
        String sql = """
                SELECT COALESCE(MAX(CAST(seatNumber AS UNSIGNED)), 0) AS m
                FROM TicketFlight
                WHERE airlineID = ? AND flightNumber = ? AND departureDateTime = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            ps.setTimestamp(3, Timestamp.valueOf(departureDateTime));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("m");
            }
        }
    }

    /**
     * Lightweight segment view used inside the cancel transaction: just the
     * flight-instance identifier and its class, no Flight join. Caller-supplied
     * connection so it participates in the same transaction as the cancel.
     */
    public record CancelSegmentRow(
            String airlineID,
            String flightNumber,
            LocalDateTime departureDateTime,
            TravelClass cls) {}

    /**
     * Full per-segment view used by the CR edit flow: includes everything
     * the editor + pricing recompute needs (basePrice from Flight, current
     * seat / class / meal from TicketFlight). Caller-supplied connection so
     * the read participates in the same txn as the subsequent updates.
     */
    public record EditSegmentRow(
            long ticketNumber,
            int segmentOrder,
            String airlineID,
            String flightNumber,
            LocalDateTime departureDateTime,
            String fromAirport,
            String toAirport,
            BigDecimal basePrice,
            TravelClass currentClass,
            String currentSeat,
            String currentMeal) {}

    public static List<EditSegmentRow> listForEdit(Connection c, int reservationID)
            throws SQLException {
        String sql = """
                SELECT t.ticketNumber, tf.segmentOrder, tf.airlineID, tf.flightNumber,
                       tf.departureDateTime, tf.seatNumber, tf.class, tf.specialMeal,
                       f.departureAirport, f.arrivalAirport, f.basePrice
                FROM Ticket       t
                JOIN TicketFlight tf ON tf.ticketNumber = t.ticketNumber
                JOIN Flight       f  ON f.airlineID = tf.airlineID
                                    AND f.flightNumber = tf.flightNumber
                WHERE t.reservationID = ?
                ORDER BY t.ticketNumber, tf.segmentOrder
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            try (ResultSet rs = ps.executeQuery()) {
                List<EditSegmentRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new EditSegmentRow(
                            rs.getLong("ticketNumber"),
                            rs.getInt("segmentOrder"),
                            rs.getString("airlineID"),
                            rs.getString("flightNumber"),
                            rs.getTimestamp("departureDateTime").toLocalDateTime(),
                            rs.getString("departureAirport"),
                            rs.getString("arrivalAirport"),
                            rs.getBigDecimal("basePrice"),
                            TravelClass.valueOf(rs.getString("class")),
                            rs.getString("seatNumber"),
                            rs.getString("specialMeal")));
                }
                return out;
            }
        }
    }

    /** Updates class / seat / specialMeal for one TicketFlight row. */
    public static int updateSegment(Connection c, long ticketNumber, int segmentOrder,
                                    TravelClass cls, String seat, String meal)
            throws SQLException {
        String sql = """
                UPDATE TicketFlight
                SET class = ?, seatNumber = ?, specialMeal = ?
                WHERE ticketNumber = ? AND segmentOrder = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, cls.name());
            ps.setString(2, seat);
            if (meal == null || meal.isBlank()) ps.setNull(3, java.sql.Types.VARCHAR);
            else                                ps.setString(3, meal);
            ps.setLong(4, ticketNumber);
            ps.setInt(5, segmentOrder);
            return ps.executeUpdate();
        }
    }

    public static List<CancelSegmentRow> listForCancel(Connection c, int reservationID)
            throws SQLException {
        String sql = """
                SELECT tf.airlineID, tf.flightNumber, tf.departureDateTime, tf.class
                FROM TicketFlight tf
                JOIN Ticket t ON t.ticketNumber = tf.ticketNumber
                WHERE t.reservationID = ?
                ORDER BY tf.ticketNumber, tf.segmentOrder
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            try (ResultSet rs = ps.executeQuery()) {
                List<CancelSegmentRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new CancelSegmentRow(
                            rs.getString("airlineID"),
                            rs.getString("flightNumber"),
                            rs.getTimestamp("departureDateTime").toLocalDateTime(),
                            TravelClass.valueOf(rs.getString("class"))));
                }
                return out;
            }
        }
    }

    /**
     * Segment-level detail for one reservation, ordered by ticket then segment.
     * Joins {@code Flight} to recover from/to airports and the scheduled
     * arrival time; arrivalDateTime is computed in Java since the Flight
     * row stores arrival as a {@code TIME} (no date), and a wall-clock
     * arrival ≤ departure means the flight crosses midnight.
     */
    public static List<SegmentDetail> listForReservation(int reservationID) {
        String sql = """
                SELECT tf.segmentOrder, tf.airlineID, tf.flightNumber,
                       tf.departureDateTime, tf.seatNumber, tf.class,
                       f.departureAirport, f.arrivalAirport,
                       f.departureTime, f.arrivalTime
                FROM TicketFlight tf
                JOIN Ticket  t ON t.ticketNumber = tf.ticketNumber
                JOIN Flight  f ON f.airlineID = tf.airlineID AND f.flightNumber = tf.flightNumber
                WHERE t.reservationID = ?
                ORDER BY tf.ticketNumber, tf.segmentOrder
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, reservationID);
            try (ResultSet rs = ps.executeQuery()) {
                List<SegmentDetail> out = new ArrayList<>();
                while (rs.next()) out.add(fromSegmentRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "listForReservation failed: reservation=" + reservationID, e);
        }
    }

    private static SegmentDetail fromSegmentRow(ResultSet rs) throws SQLException {
        LocalDateTime dep = rs.getTimestamp("departureDateTime").toLocalDateTime();
        LocalTime depTime = rs.getTime("departureTime").toLocalTime();
        LocalTime arrTime = rs.getTime("arrivalTime").toLocalTime();
        Duration trip = Duration.between(depTime, arrTime);
        if (!trip.isPositive()) trip = trip.plusDays(1);
        return new SegmentDetail(
                rs.getInt("segmentOrder"),
                rs.getString("airlineID"),
                rs.getString("flightNumber"),
                rs.getString("departureAirport"),
                rs.getString("arrivalAirport"),
                dep,
                dep.plus(trip),
                TravelClass.valueOf(rs.getString("class")),
                rs.getString("seatNumber"));
    }
}
