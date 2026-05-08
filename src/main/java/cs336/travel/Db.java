package cs336.travel;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Single source of truth for JDBC connection params and a smoke-test main.
 *
 * <p>Edit {@link #URL}, {@link #USER}, {@link #PASSWORD} for your machine.
 *
 * <p>House style: every DAO obtains a Connection via {@link #getConnection()},
 * uses try-with-resources, and never holds the connection across method
 * boundaries. No connection pooling — a class project doesn't need it.
 */
public final class Db {

    public static final String URL =
            "jdbc:mysql://localhost:3306/travelres?serverTimezone=UTC&allowPublicKeyRetrieval=true&useSSL=false";
    public static final String USER     = "root";
    public static final String PASSWORD = "changeme";    // sanitized for submission — set to your local MySQL root password

    private Db() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /** Smoke test: {@code mvn exec:java -Dexec.mainClass=cs336.travel.Db} */
    public static void main(String[] args) throws SQLException {
        try (Connection c  = getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM Employee");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            System.out.println("Connected to travelres at " + URL);
            System.out.println("Found " + rs.getInt(1) + " employee row(s).");
        }
    }
}
