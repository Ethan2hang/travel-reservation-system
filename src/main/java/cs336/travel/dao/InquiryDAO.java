package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.InquiryListRow;
import cs336.travel.model.InquiryRow;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class InquiryDAO {

    private InquiryDAO() {}

    /** Inserts a new question; schema defaults {@code postedAt=NOW()} and {@code status='OPEN'}. */
    public static int insertOpen(int customerID, String question) {
        String sql = "INSERT INTO Inquiry (customerID, question) VALUES (?, ?)";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, customerID);
            ps.setString(2, question);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Inquiry insert returned no key");
                return keys.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("insertOpen failed: customer=" + customerID, e);
        }
    }

    public static List<InquiryRow> listForCustomer(int customerID) {
        String sql = """
                SELECT inquiryID, question, postedAt, answer, answeredAt, status
                FROM Inquiry
                WHERE customerID = ?
                ORDER BY postedAt DESC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                List<InquiryRow> out = new ArrayList<>();
                while (rs.next()) out.add(fromRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("listForCustomer failed: customer=" + customerID, e);
        }
    }

    /** Inquiries posted by this customer, any status. */
    public static int countForCustomer(int customerID) {
        String sql = "SELECT COUNT(*) AS cnt FROM Inquiry WHERE customerID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countForCustomer (inquiry) failed: " + customerID, e);
        }
    }

    /** Inquiries answered by this employee — blocks rep deletion. */
    public static int countAnsweredByEmployee(int employeeID) {
        String sql = "SELECT COUNT(*) AS cnt FROM Inquiry WHERE answeredBy = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, employeeID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countAnsweredByEmployee failed: " + employeeID, e);
        }
    }

    public static List<InquiryListRow> listAllOpen() {
        String sql = """
                SELECT i.inquiryID, i.customerID, c.username AS customerUsername,
                       c.name AS customerName, i.question, i.postedAt,
                       i.answer, i.answeredAt, NULL AS answeredByUsername, i.status
                FROM Inquiry  i
                JOIN Customer c ON c.customerID = i.customerID
                WHERE i.status = 'OPEN'
                ORDER BY i.postedAt ASC, i.inquiryID ASC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<InquiryListRow> out = new ArrayList<>();
            while (rs.next()) out.add(fromListRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("listAllOpen failed", e);
        }
    }

    public static List<InquiryListRow> listAllAnswered() {
        String sql = """
                SELECT i.inquiryID, i.customerID, c.username AS customerUsername,
                       c.name AS customerName, i.question, i.postedAt,
                       i.answer, i.answeredAt,
                       e.username AS answeredByUsername, i.status
                FROM Inquiry       i
                JOIN Customer      c ON c.customerID = i.customerID
                LEFT JOIN Employee e ON e.employeeID = i.answeredBy
                WHERE i.status = 'ANSWERED'
                ORDER BY i.answeredAt DESC, i.inquiryID DESC
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<InquiryListRow> out = new ArrayList<>();
            while (rs.next()) out.add(fromListRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("listAllAnswered failed", e);
        }
    }

    /**
     * Caller-supplied connection variant. Guarded by {@code status='OPEN'}
     * so two reps clicking Send at once produce one update + one no-op
     * (caller treats 0 rows as "already answered"). Sets {@code answeredAt
     * = NOW()} via SQL.
     */
    public static int reply(Connection c, int inquiryID, int answeredByEmployeeID, String answer)
            throws SQLException {
        String sql = """
                UPDATE Inquiry
                SET answer = ?, answeredBy = ?, answeredAt = NOW(), status = 'ANSWERED'
                WHERE inquiryID = ? AND status = 'OPEN'
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, answer);
            ps.setInt(2, answeredByEmployeeID);
            ps.setInt(3, inquiryID);
            return ps.executeUpdate();
        }
    }

    /** Looks up which customer owns an inquiry — used to address the reply notification. */
    public static java.util.Optional<Integer> findCustomerId(Connection c, int inquiryID)
            throws SQLException {
        String sql = "SELECT customerID FROM Inquiry WHERE inquiryID = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, inquiryID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? java.util.Optional.of(rs.getInt(1)) : java.util.Optional.empty();
            }
        }
    }

    private static InquiryListRow fromListRow(ResultSet rs) throws SQLException {
        Timestamp answeredAt = rs.getTimestamp("answeredAt");
        return new InquiryListRow(
                rs.getInt("inquiryID"),
                rs.getInt("customerID"),
                rs.getString("customerUsername"),
                rs.getString("customerName"),
                rs.getString("question"),
                rs.getTimestamp("postedAt").toLocalDateTime(),
                rs.getString("answer"),
                answeredAt == null ? null : answeredAt.toLocalDateTime(),
                rs.getString("answeredByUsername"),
                rs.getString("status"));
    }

    private static InquiryRow fromRow(ResultSet rs) throws SQLException {
        Timestamp answeredAt = rs.getTimestamp("answeredAt");
        return new InquiryRow(
                rs.getInt("inquiryID"),
                rs.getString("question"),
                rs.getTimestamp("postedAt").toLocalDateTime(),
                rs.getString("answer"),
                answeredAt == null ? null : answeredAt.toLocalDateTime(),
                rs.getString("status"));
    }
}
