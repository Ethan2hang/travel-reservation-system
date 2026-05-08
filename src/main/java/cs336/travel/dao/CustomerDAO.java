package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CustomerDAO {

    private CustomerDAO() {}

    public static Optional<Customer> findByUsername(String username) {
        String sql = """
                SELECT customerID, username, name, email, phone
                FROM Customer
                WHERE username = ?
                """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fromRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUsername failed: " + username, e);
        }
    }

    public static Optional<Customer> findById(int customerID) {
        String sql = """
                SELECT customerID, username, name, email, phone
                FROM Customer
                WHERE customerID = ?
                """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fromRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + customerID, e);
        }
    }

    public static List<Customer> listAll() {
        String sql = """
                SELECT customerID, username, name, email, phone
                FROM Customer
                ORDER BY customerID
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Customer> out = new ArrayList<>();
            while (rs.next()) out.add(fromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("listAll customers failed", e);
        }
    }

    /** Inserts a Customer. Translates UNIQUE collisions on username/email into a typed exception. */
    public static int insertCustomer(String username, String password, String name,
                                     String email, String phone) {
        String sql = """
                INSERT INTO Customer (username, password, name, email, phone)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            ps.setString(4, email);
            if (phone == null || phone.isBlank()) ps.setNull(5, java.sql.Types.VARCHAR);
            else                                  ps.setString(5, phone);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Customer insert returned no key");
                return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateFieldException(translateDup(dup));
        } catch (SQLException e) {
            throw new RuntimeException("insertCustomer failed: " + username, e);
        }
    }

    /** Update; blank password keeps existing. */
    public static int updateCustomer(int customerID, String username, String newPassword,
                                     String name, String email, String phone) {
        boolean changePw = newPassword != null && !newPassword.isBlank();
        String sql = changePw
                ? "UPDATE Customer SET username=?, password=?, name=?, email=?, phone=? WHERE customerID=?"
                : "UPDATE Customer SET username=?, name=?, email=?, phone=? WHERE customerID=?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, username);
            if (changePw) ps.setString(i++, newPassword);
            ps.setString(i++, name);
            ps.setString(i++, email);
            if (phone == null || phone.isBlank()) ps.setNull(i++, java.sql.Types.VARCHAR);
            else                                  ps.setString(i++, phone);
            ps.setInt(i, customerID);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateFieldException(translateDup(dup));
        } catch (SQLException e) {
            throw new RuntimeException("updateCustomer failed: id=" + customerID, e);
        }
    }

    public static int deleteCustomer(int customerID) {
        String sql = "DELETE FROM Customer WHERE customerID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, customerID);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteCustomer failed: id=" + customerID, e);
        }
    }

    private static String translateDup(SQLIntegrityConstraintViolationException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (msg.contains("email"))    return "Email already in use.";
        if (msg.contains("username")) return "Username already in use.";
        return "That value is already in use.";
    }

    private static Customer fromRow(ResultSet rs) throws SQLException {
        return new Customer(
                rs.getInt("customerID"),
                rs.getString("username"),
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("phone"));
    }

    /** Marker for unique-constraint violations on Customer.username / Customer.email. */
    public static final class DuplicateFieldException extends RuntimeException {
        public DuplicateFieldException(String m) { super(m); }
    }
}
