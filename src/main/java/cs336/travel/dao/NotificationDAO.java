package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.NotificationItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public final class NotificationDAO {

    private NotificationDAO() {}

    /** Caller-supplied connection variant — used inside the cancel transaction. */
    public static int insert(Connection c, int customerID, String message) throws SQLException {
        String sql = "INSERT INTO Notification (customerID, message) VALUES (?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerID);
            ps.setString(2, message);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Notification insert returned no key");
                return keys.getInt(1);
            }
        }
    }

    public static List<NotificationItem> listUnreadFor(int customerID) {
        String sql = """
                SELECT notificationID, message, createdAt
                FROM Notification
                WHERE customerID = ? AND readAt IS NULL
                ORDER BY createdAt ASC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                List<NotificationItem> out = new ArrayList<>();
                while (rs.next()) out.add(fromRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("listUnreadFor failed: customer=" + customerID, e);
        }
    }

    /** All notifications addressed to this customer, regardless of read state. */
    public static int countForCustomer(int customerID) {
        String sql = "SELECT COUNT(*) AS cnt FROM Notification WHERE customerID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countForCustomer (notification) failed: " + customerID, e);
        }
    }

    /** Stamps {@code readAt = NOW()} on every unread row for this customer. */
    public static int markAllRead(int customerID) {
        String sql = "UPDATE Notification SET readAt = NOW() WHERE customerID = ? AND readAt IS NULL";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("markAllRead failed: customer=" + customerID, e);
        }
    }

    private static NotificationItem fromRow(ResultSet rs) throws SQLException {
        return new NotificationItem(
                rs.getInt("notificationID"),
                rs.getString("message"),
                rs.getTimestamp("createdAt").toLocalDateTime());
    }
}
