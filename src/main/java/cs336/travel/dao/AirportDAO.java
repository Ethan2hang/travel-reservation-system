package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.Airport;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AirportDAO {

    private AirportDAO() {}

    public static List<Airport> listAll() {
        String sql = """
                SELECT airportID, name, city, country
                FROM Airport
                ORDER BY airportID
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Airport> out = new ArrayList<>();
            while (rs.next()) out.add(fromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("AirportDAO.listAll failed", e);
        }
    }

    public static Optional<Airport> findById(String airportID) {
        String sql = "SELECT airportID, name, city, country FROM Airport WHERE airportID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airportID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fromRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + airportID, e);
        }
    }

    public static int insert(String airportID, String name, String city, String country) {
        String sql = "INSERT INTO Airport (airportID, name, city, country) VALUES (?, ?, ?, ?)";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airportID);
            ps.setString(2, name);
            ps.setString(3, city);
            ps.setString(4, country);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateKeyException("Airport ID already in use.");
        } catch (SQLException e) {
            throw new RuntimeException("insert airport failed: " + airportID, e);
        }
    }

    public static int update(String airportID, String name, String city, String country) {
        String sql = "UPDATE Airport SET name = ?, city = ?, country = ? WHERE airportID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, city);
            ps.setString(3, country);
            ps.setString(4, airportID);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update airport failed: " + airportID, e);
        }
    }

    /**
     * Rename + update. Schema has no ON UPDATE CASCADE on Flight's airport FKs,
     * so the service layer must pre-check that no Flight uses the old code
     * before this fires. {@link DuplicateKeyException} on PK collision.
     */
    public static int updateWithRename(String oldAirportID, String newAirportID,
                                       String name, String city, String country) {
        String sql = "UPDATE Airport SET airportID = ?, name = ?, city = ?, country = ? "
                   + "WHERE airportID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newAirportID);
            ps.setString(2, name);
            ps.setString(3, city);
            ps.setString(4, country);
            ps.setString(5, oldAirportID);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateKeyException("Airport ID already in use.");
        } catch (SQLException e) {
            throw new RuntimeException(
                    "updateWithRename airport failed: " + oldAirportID + "→" + newAirportID, e);
        }
    }

    public static int delete(String airportID) {
        String sql = "DELETE FROM Airport WHERE airportID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airportID);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete airport failed: " + airportID, e);
        }
    }

    /** Flights that depart from OR arrive at this airport. */
    public static int countFlightsAt(String airportID) {
        String sql = "SELECT COUNT(*) AS cnt FROM Flight "
                   + "WHERE departureAirport = ? OR arrivalAirport = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airportID);
            ps.setString(2, airportID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countFlightsAt failed: " + airportID, e);
        }
    }

    private static Airport fromRow(ResultSet rs) throws SQLException {
        return new Airport(
                rs.getString("airportID"),
                rs.getString("name"),
                rs.getString("city"),
                rs.getString("country"));
    }

    /** Marker for unique-constraint violations on Airport.airportID. */
    public static final class DuplicateKeyException extends RuntimeException {
        public DuplicateKeyException(String m) { super(m); }
    }
}
