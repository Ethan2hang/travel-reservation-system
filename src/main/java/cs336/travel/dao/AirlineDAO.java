package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.Airline;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AirlineDAO {

    private AirlineDAO() {}

    public static List<Airline> listAll() {
        String sql = """
                SELECT airlineID, airlineName
                FROM Airline
                ORDER BY airlineID
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Airline> out = new ArrayList<>();
            while (rs.next()) out.add(fromRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("AirlineDAO.listAll failed", e);
        }
    }

    private static Airline fromRow(ResultSet rs) throws SQLException {
        return new Airline(
                rs.getString("airlineID"),
                rs.getString("airlineName"));
    }
}
