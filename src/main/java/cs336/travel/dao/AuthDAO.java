package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.Customer;
import cs336.travel.model.Employee;
import cs336.travel.model.Role;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/**
 * Credential checks. Kept separate from {@link CustomerDAO} / {@link EmployeeDAO}
 * so the password column never flows through the public model records.
 *
 * <p>Plaintext password match is intentional for this class project — see
 * {@code docs/SCHEMA.md} "Open questions".
 */
public final class AuthDAO {

    private AuthDAO() {}

    public static Optional<Employee> authenticateEmployee(String username, String password) {
        String sql = """
                SELECT employeeID, username, name, role
                FROM Employee
                WHERE username = ? AND password = ?
                """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Employee(
                        rs.getInt("employeeID"),
                        rs.getString("username"),
                        rs.getString("name"),
                        Role.valueOf(rs.getString("role"))));
            }
        } catch (SQLException e) {
            throw new RuntimeException("authenticateEmployee failed: " + username, e);
        }
    }

    public static Optional<Customer> authenticateCustomer(String username, String password) {
        String sql = """
                SELECT customerID, username, name, email, phone
                FROM Customer
                WHERE username = ? AND password = ?
                """;
        try (Connection c = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return Optional.empty();
                return Optional.of(new Customer(
                        rs.getInt("customerID"),
                        rs.getString("username"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("phone")));
            }
        } catch (SQLException e) {
            throw new RuntimeException("authenticateCustomer failed: " + username, e);
        }
    }
}
