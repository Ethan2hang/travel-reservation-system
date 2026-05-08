package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.Aircraft;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AircraftDAO {

    private AircraftDAO() {}

    public static List<Aircraft> listAll() {
        String sql = """
                SELECT aircraftID, airlineID, seatCapacity
                FROM Aircraft
                ORDER BY airlineID, aircraftID
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Aircraft> out = new ArrayList<>();
            while (rs.next()) out.add(fromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("AircraftDAO.listAll failed", e);
        }
    }

    public static List<Aircraft> listForAirline(String airlineID) {
        String sql = """
                SELECT aircraftID, airlineID, seatCapacity
                FROM Aircraft
                WHERE airlineID = ?
                ORDER BY aircraftID
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            try (ResultSet rs = ps.executeQuery()) {
                List<Aircraft> out = new ArrayList<>();
                while (rs.next()) out.add(fromRow(rs));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("listForAirline failed: " + airlineID, e);
        }
    }

    public static Optional<Aircraft> findById(String aircraftID) {
        String sql = "SELECT aircraftID, airlineID, seatCapacity FROM Aircraft WHERE aircraftID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, aircraftID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fromRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + aircraftID, e);
        }
    }

    public static int insert(String aircraftID, String airlineID, int seatCapacity) {
        String sql = "INSERT INTO Aircraft (aircraftID, airlineID, seatCapacity) VALUES (?, ?, ?)";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, aircraftID);
            ps.setString(2, airlineID);
            ps.setInt(3, seatCapacity);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateKeyException("Aircraft ID already in use.");
        } catch (SQLException e) {
            throw new RuntimeException("insert aircraft failed: " + aircraftID, e);
        }
    }

    public static int update(String aircraftID, String airlineID, int seatCapacity) {
        String sql = "UPDATE Aircraft SET airlineID = ?, seatCapacity = ? WHERE aircraftID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setInt(2, seatCapacity);
            ps.setString(3, aircraftID);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update aircraft failed: " + aircraftID, e);
        }
    }

    /**
     * Rename + update — also sets aircraftID. Schema has no ON UPDATE CASCADE
     * on Flight.aircraftID, so the service layer must pre-check that no Flight
     * references the old ID (or the foreign key constraint will reject).
     * Throws {@link DuplicateKeyException} if newAircraftID already exists.
     */
    public static int updateWithRename(String oldAircraftID, String newAircraftID,
                                       String airlineID, int seatCapacity) {
        String sql = "UPDATE Aircraft SET aircraftID = ?, airlineID = ?, seatCapacity = ? "
                   + "WHERE aircraftID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, newAircraftID);
            ps.setString(2, airlineID);
            ps.setInt(3, seatCapacity);
            ps.setString(4, oldAircraftID);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateKeyException("Aircraft ID already in use.");
        } catch (SQLException e) {
            throw new RuntimeException(
                    "updateWithRename aircraft failed: " + oldAircraftID + "→" + newAircraftID, e);
        }
    }

    public static int delete(String aircraftID) {
        String sql = "DELETE FROM Aircraft WHERE aircraftID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, aircraftID);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete aircraft failed: " + aircraftID, e);
        }
    }

    public static int countFlightsUsing(String aircraftID) {
        String sql = "SELECT COUNT(*) AS cnt FROM Flight WHERE aircraftID = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, aircraftID);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countFlightsUsing failed: " + aircraftID, e);
        }
    }

    /**
     * Returns {@code Aircraft.seatCapacity} for the aircraft assigned to the
     * given (airlineID, flightNumber). Throws if the flight or its aircraft
     * row is missing — both are FK-enforced, so absence is a programming error.
     */
    public static int getCapacityForFlight(Connection c, String airlineID, String flightNumber)
            throws SQLException {
        String sql = """
                SELECT a.seatCapacity AS cap
                FROM Flight f
                JOIN Aircraft a ON a.aircraftID = f.aircraftID
                WHERE f.airlineID = ? AND f.flightNumber = ?
                """;
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("No aircraft for flight " + airlineID + flightNumber);
                }
                return rs.getInt("cap");
            }
        }
    }

    public static int getCapacityForFlight(String airlineID, String flightNumber) {
        try (Connection c = Db.getConnection()) {
            return getCapacityForFlight(c, airlineID, flightNumber);
        } catch (SQLException e) {
            throw new RuntimeException(
                    "getCapacityForFlight failed: " + airlineID + flightNumber, e);
        }
    }

    private static Aircraft fromRow(ResultSet rs) throws SQLException {
        return new Aircraft(
                rs.getString("aircraftID"),
                rs.getString("airlineID"),
                rs.getInt("seatCapacity"));
    }

    /** Marker for unique-constraint violations on Aircraft.aircraftID. */
    public static final class DuplicateKeyException extends RuntimeException {
        public DuplicateKeyException(String m) { super(m); }
    }
}
