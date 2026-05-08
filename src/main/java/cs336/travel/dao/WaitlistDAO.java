package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.PromotedEntry;
import cs336.travel.model.TravelClass;
import cs336.travel.model.WaitlistRow;

import java.util.ArrayList;
import java.util.List;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;

public final class WaitlistDAO {

    private WaitlistDAO() {}

    /**
     * Caller-supplied connection variant for transactional round-trip waitlist.
     * Returns generated {@code waitlistID}. Schema defaults
     * {@code requestDateTime=NOW()} and {@code status='WAITING'}.
     */
    public static int insertEntry(Connection c,
                                  int customerID,
                                  String airlineID,
                                  String flightNumber,
                                  LocalDate flightDate,
                                  TravelClass cls) throws SQLException {
        String sql = """
                INSERT INTO WaitlistEntry
                  (customerID, airlineID, flightNumber, flightDate, class)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerID);
            ps.setString(2, airlineID);
            ps.setString(3, flightNumber);
            ps.setDate(4, Date.valueOf(flightDate));
            ps.setString(5, cls.name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("WaitlistEntry insert returned no key");
                return keys.getInt(1);
            }
        }
    }

    public static int insertEntry(int customerID, String airlineID, String flightNumber,
                                  LocalDate flightDate, TravelClass cls) {
        try (Connection c = Db.getConnection()) {
            return insertEntry(c, customerID, airlineID, flightNumber, flightDate, cls);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "insertEntry failed: customer=" + customerID
                            + " " + airlineID + flightNumber + " " + flightDate, e);
        }
    }

    /** Count of {@code WAITING} entries on this flight instance — used to display position. */
    public static int countWaiting(String airlineID, String flightNumber, LocalDate flightDate) {
        String sql = """
                SELECT COUNT(*) AS cnt
                FROM WaitlistEntry
                WHERE airlineID = ? AND flightNumber = ? AND flightDate = ? AND status = 'WAITING'
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            ps.setDate(3, Date.valueOf(flightDate));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "countWaiting failed: " + airlineID + flightNumber + " " + flightDate, e);
        }
    }

    /** All waitlist rows owned by this customer, regardless of status. */
    public static int countForCustomer(int customerID) {
        String sql = "SELECT COUNT(*) AS cnt FROM WaitlistEntry WHERE customerID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countForCustomer (waitlist) failed: " + customerID, e);
        }
    }

    /** Same query as {@link #countWaiting}, but on a caller-supplied connection. */
    public static int countWaiting(Connection c, String airlineID, String flightNumber,
                                   LocalDate flightDate) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS cnt
                FROM WaitlistEntry
                WHERE airlineID = ? AND flightNumber = ? AND flightDate = ? AND status = 'WAITING'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            ps.setDate(3, Date.valueOf(flightDate));
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        }
    }

    /**
     * Promotes the oldest WAITING entry on this flight instance from
     * {@code WAITING} to {@code PROMOTED}. Returns the promoted row, or
     * empty if no one is waiting. Caller-supplied connection — used inside
     * the cancellation transaction so the promotion rolls back together
     * with the cancel itself if anything downstream fails.
     */
    public static Optional<PromotedEntry> promoteFirstWaiting(
            Connection c, String airlineID, String flightNumber, LocalDate flightDate)
            throws SQLException {
        String selectSql = """
                SELECT waitlistID, customerID, class
                FROM WaitlistEntry
                WHERE airlineID = ? AND flightNumber = ? AND flightDate = ? AND status = 'WAITING'
                ORDER BY waitlistID ASC
                LIMIT 1
                """;
        int waitlistID;
        int customerID;
        TravelClass cls;
        try (PreparedStatement ps = c.prepareStatement(selectSql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            ps.setDate(3, Date.valueOf(flightDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                waitlistID = rs.getInt("waitlistID");
                customerID = rs.getInt("customerID");
                cls        = TravelClass.valueOf(rs.getString("class"));
            }
        }
        String updateSql =
                "UPDATE WaitlistEntry SET status = 'PROMOTED' WHERE waitlistID = ? AND status = 'WAITING'";
        try (PreparedStatement ps = c.prepareStatement(updateSql)) {
            ps.setInt(1, waitlistID);
            int rows = ps.executeUpdate();
            if (rows != 1) {
                // Lost a race with another promotion — caller should treat as no-op.
                return Optional.empty();
            }
        }
        return Optional.of(new PromotedEntry(
                waitlistID, customerID, airlineID, flightNumber, flightDate, cls));
    }

    /**
     * All waitlist entries for one flight instance, joined with Customer.
     * Ordered oldest-first so the CR sees the queue order. Includes all
     * statuses (WAITING / PROMOTED / EXPIRED) for an honest audit view.
     */
    public static List<WaitlistRow> listForFlight(String airlineID, String flightNumber,
                                                  LocalDate flightDate) {
        String sql = """
                SELECT we.waitlistID, c.username, c.name AS customerName,
                       c.email, c.phone,
                       we.class AS class, we.status, we.requestDateTime
                FROM WaitlistEntry we
                JOIN Customer      c ON c.customerID = we.customerID
                WHERE we.airlineID = ? AND we.flightNumber = ? AND we.flightDate = ?
                ORDER BY we.requestDateTime ASC, we.waitlistID ASC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            ps.setDate(3, Date.valueOf(flightDate));
            try (ResultSet rs = ps.executeQuery()) {
                List<WaitlistRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new WaitlistRow(
                            rs.getInt("waitlistID"),
                            rs.getString("username"),
                            rs.getString("customerName"),
                            rs.getString("email"),
                            rs.getString("phone"),
                            TravelClass.valueOf(rs.getString("class")),
                            rs.getString("status"),
                            rs.getTimestamp("requestDateTime").toLocalDateTime()));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "listForFlight failed: " + airlineID + flightNumber + " " + flightDate, e);
        }
    }
}
