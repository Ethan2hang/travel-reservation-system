package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.TripType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class TicketDAO {

    private TicketDAO() {}

    /**
     * Caller-supplied connection variant for transactional booking.
     * Returns the generated {@code ticketNumber} (BIGINT). Schema defaults
     * {@code purchaseDateTime=NOW()}.
     */
    public static long insertTicket(Connection c,
                                    int reservationID,
                                    TripType tripType,
                                    BigDecimal totalFare,
                                    BigDecimal bookingFee) throws SQLException {
        String sql = """
                INSERT INTO Ticket (reservationID, tripType, totalFare, bookingFee)
                VALUES (?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, reservationID);
            ps.setString(2, tripType.name());
            ps.setBigDecimal(3, totalFare);
            ps.setBigDecimal(4, bookingFee);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Ticket insert returned no key");
                return keys.getLong(1);
            }
        }
    }

    /** Updates the cached fare on a Ticket row — used after a segment-class edit. */
    public static int updateTotalFare(Connection c, long ticketNumber, BigDecimal newFare)
            throws SQLException {
        String sql = "UPDATE Ticket SET totalFare = ? WHERE ticketNumber = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setBigDecimal(1, newFare);
            ps.setLong(2, ticketNumber);
            return ps.executeUpdate();
        }
    }

    public static long insertTicket(int reservationID, TripType tripType,
                                    BigDecimal totalFare, BigDecimal bookingFee) {
        try (Connection c = Db.getConnection()) {
            return insertTicket(c, reservationID, tripType, totalFare, bookingFee);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "insertTicket failed: reservation=" + reservationID + " " + tripType, e);
        }
    }
}
