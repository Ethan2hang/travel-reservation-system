package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.Employee;
import cs336.travel.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class EmployeeDAO {

    private EmployeeDAO() {}

    public static Optional<Employee> findByUsername(String username) {
        String sql = """
                SELECT employeeID, username, name, role
                FROM Employee
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

    public static Optional<Employee> findById(int employeeID) {
        String sql = """
                SELECT employeeID, username, name, role
                FROM Employee
                WHERE employeeID = ?
                """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, employeeID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fromRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + employeeID, e);
        }
    }

    public static List<Employee> listCustomerReps() {
        String sql = """
                SELECT employeeID, username, name, role
                FROM Employee
                WHERE role = 'CUSTOMER_REP'
                ORDER BY employeeID
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Employee> out = new ArrayList<>();
            while (rs.next()) out.add(fromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("listCustomerReps failed", e);
        }
    }

    /** Inserts a new CUSTOMER_REP. Throws {@link DuplicateUsernameException} on UNIQUE collision. */
    public static int insertCustomerRep(String username, String password, String name) {
        String sql = "INSERT INTO Employee (username, password, name, role) VALUES (?, ?, ?, 'CUSTOMER_REP')";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, username);
            ps.setString(2, password);
            ps.setString(3, name);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) throw new SQLException("Employee insert returned no key");
                return keys.getInt(1);
            }
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateUsernameException("Username already in use.");
        } catch (SQLException e) {
            throw new RuntimeException("insertCustomerRep failed: " + username, e);
        }
    }

    /**
     * Updates an Employee. {@code newPassword} null/blank means "keep existing
     * password" — prevents an Edit dialog with a blank password field from
     * silently wiping the credential.
     */
    public static int updateEmployee(int employeeID, String username,
                                     String newPassword, String name) {
        boolean changePw = newPassword != null && !newPassword.isBlank();
        String sql = changePw
                ? "UPDATE Employee SET username=?, password=?, name=? WHERE employeeID=?"
                : "UPDATE Employee SET username=?, name=? WHERE employeeID=?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1;
            ps.setString(i++, username);
            if (changePw) ps.setString(i++, newPassword);
            ps.setString(i++, name);
            ps.setInt(i, employeeID);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateUsernameException("Username already in use.");
        } catch (SQLException e) {
            throw new RuntimeException("updateEmployee failed: id=" + employeeID, e);
        }
    }

    public static int deleteEmployee(int employeeID) {
        String sql = "DELETE FROM Employee WHERE employeeID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, employeeID);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteEmployee failed: id=" + employeeID, e);
        }
    }

    private static Employee fromRow(ResultSet rs) throws SQLException {
        return new Employee(
                rs.getInt("employeeID"),
                rs.getString("username"),
                rs.getString("name"),
                Role.valueOf(rs.getString("role")));
    }

    /** Marker for unique-constraint violations on Employee.username. */
    public static final class DuplicateUsernameException extends RuntimeException {
        public DuplicateUsernameException(String m) { super(m); }
    }
}
