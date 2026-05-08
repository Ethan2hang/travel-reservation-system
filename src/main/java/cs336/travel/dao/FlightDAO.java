package cs336.travel.dao;

import cs336.travel.Db;
import cs336.travel.model.AirportFlightRow;
import cs336.travel.model.Flight;
import cs336.travel.model.FlightSearchResult;
import cs336.travel.model.TravelClass;
import cs336.travel.service.PricingService;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class FlightDAO {

    // Multi-segment itineraries out of scope; all search results are direct
    // flights. The Stops column always renders 0.
    private static final int STOPS_DIRECT = 0;

    private FlightDAO() {}

    public static List<FlightSearchResult> searchOneWay(
            String fromAirport, String toAirport, LocalDate date, TravelClass cls) {
        String sql = """
                SELECT f.airlineID, a.airlineName, f.flightNumber,
                       f.departureAirport, f.arrivalAirport,
                       f.departureTime, f.arrivalTime, f.basePrice
                FROM Flight f
                JOIN Airline a ON a.airlineID = f.airlineID
                WHERE f.departureAirport = ?
                  AND f.arrivalAirport   = ?
                  AND FIND_IN_SET(?, f.operatingDays) > 0
                ORDER BY f.departureTime
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fromAirport);
            ps.setString(2, toAirport);
            ps.setString(3, dayToken(date.getDayOfWeek()));
            try (ResultSet rs = ps.executeQuery()) {
                List<FlightSearchResult> out = new ArrayList<>();
                while (rs.next()) out.add(fromRow(rs, date, cls));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "searchOneWay failed: " + fromAirport + "->" + toAirport
                            + " on " + date + " (" + cls + ")", e);
        }
    }

    public static List<FlightSearchResult> searchOneWayFlexible(
            String fromAirport, String toAirport, LocalDate centerDate,
            int plusMinusDays, TravelClass cls) {
        String sql = """
                SELECT f.airlineID, a.airlineName, f.flightNumber,
                       f.departureAirport, f.arrivalAirport,
                       f.departureTime, f.arrivalTime, f.basePrice,
                       f.operatingDays
                FROM Flight f
                JOIN Airline a ON a.airlineID = f.airlineID
                WHERE f.departureAirport = ?
                  AND f.arrivalAirport   = ?
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fromAirport);
            ps.setString(2, toAirport);
            try (ResultSet rs = ps.executeQuery()) {
                List<FlightRow> flights = new ArrayList<>();
                while (rs.next()) flights.add(toFlightRow(rs));

                List<FlightSearchResult> out = new ArrayList<>();
                for (int offset = -plusMinusDays; offset <= plusMinusDays; offset++) {
                    LocalDate d = centerDate.plusDays(offset);
                    String token = dayToken(d.getDayOfWeek());
                    for (FlightRow f : flights) {
                        if (operatingOn(f.operatingDays, token)) {
                            out.add(f.toResult(d, cls));
                        }
                    }
                }
                out.sort(Comparator
                        .comparing(FlightSearchResult::flightDate)
                        .thenComparing(FlightSearchResult::departureTime));
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException(
                    "searchOneWayFlexible failed: " + fromAirport + "->" + toAirport
                            + " around " + centerDate + " ±" + plusMinusDays
                            + " (" + cls + ")", e);
        }
    }

    private static FlightSearchResult fromRow(ResultSet rs, LocalDate flightDate, TravelClass cls)
            throws SQLException {
        LocalTime dep = rs.getTime("departureTime").toLocalTime();
        LocalTime arr = rs.getTime("arrivalTime").toLocalTime();
        BigDecimal base = rs.getBigDecimal("basePrice");
        return new FlightSearchResult(
                flightDate,
                rs.getString("airlineID"),
                rs.getString("airlineName"),
                rs.getString("flightNumber"),
                rs.getString("departureAirport"),
                rs.getString("arrivalAirport"),
                dep,
                arr,
                computeDuration(dep, arr),
                STOPS_DIRECT,
                cls,
                base,
                PricingService.priceFor(base, cls));
    }

    private static FlightRow toFlightRow(ResultSet rs) throws SQLException {
        return new FlightRow(
                rs.getString("airlineID"),
                rs.getString("airlineName"),
                rs.getString("flightNumber"),
                rs.getString("departureAirport"),
                rs.getString("arrivalAirport"),
                rs.getTime("departureTime").toLocalTime(),
                rs.getTime("arrivalTime").toLocalTime(),
                rs.getBigDecimal("basePrice"),
                rs.getString("operatingDays"));
    }

    /**
     * Wall-clock arrival ≤ departure ⇒ next-day arrival, e.g. 22:30 → 06:30
     * is 8h00m, not -16h. Schedule data has no explicit date for the
     * arrival, so we treat any non-positive same-day delta as midnight crossing.
     */
    private static Duration computeDuration(LocalTime dep, LocalTime arr) {
        Duration d = Duration.between(dep, arr);
        if (!d.isPositive()) {
            d = d.plusDays(1);
        }
        return d;
    }

    private static boolean operatingOn(String operatingDays, String dayToken) {
        for (String s : operatingDays.split(",")) {
            if (s.trim().equals(dayToken)) return true;
        }
        return false;
    }

    private static String dayToken(DayOfWeek d) {
        return switch (d) {
            case MONDAY    -> "MON";
            case TUESDAY   -> "TUE";
            case WEDNESDAY -> "WED";
            case THURSDAY  -> "THU";
            case FRIDAY    -> "FRI";
            case SATURDAY  -> "SAT";
            case SUNDAY    -> "SUN";
        };
    }

    public static List<Flight> listAllAdmin() {
        String sql = """
                SELECT airlineID, flightNumber, aircraftID,
                       departureAirport, arrivalAirport,
                       departureTime, arrivalTime,
                       operatingDays, isDomestic, basePrice
                FROM Flight
                ORDER BY airlineID, flightNumber
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<Flight> out = new ArrayList<>();
            while (rs.next()) out.add(fromAdminRow(rs));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("listAllAdmin failed", e);
        }
    }

    public static Optional<Flight> findById(String airlineID, String flightNumber) {
        String sql = """
                SELECT airlineID, flightNumber, aircraftID,
                       departureAirport, arrivalAirport,
                       departureTime, arrivalTime,
                       operatingDays, isDomestic, basePrice
                FROM Flight
                WHERE airlineID = ? AND flightNumber = ?
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(fromAdminRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById failed: " + airlineID + flightNumber, e);
        }
    }

    public static int insert(Flight f) {
        String sql = """
                INSERT INTO Flight
                  (airlineID, flightNumber, aircraftID,
                   departureAirport, arrivalAirport,
                   departureTime, arrivalTime,
                   operatingDays, isDomestic, basePrice)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            bindFlight(ps, f);
            return ps.executeUpdate();
        } catch (SQLIntegrityConstraintViolationException dup) {
            throw new DuplicateKeyException("Flight already exists for that airline.");
        } catch (SQLException e) {
            throw new RuntimeException("insert flight failed: " + f.airlineID() + f.flightNumber(), e);
        }
    }

    /** Updates a Flight; PK (airlineID, flightNumber) is the locator and stays fixed. */
    public static int update(Flight f) {
        String sql = """
                UPDATE Flight
                SET aircraftID = ?, departureAirport = ?, arrivalAirport = ?,
                    departureTime = ?, arrivalTime = ?,
                    operatingDays = ?, isDomestic = ?, basePrice = ?
                WHERE airlineID = ? AND flightNumber = ?
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, f.aircraftID());
            ps.setString(2, f.departureAirport());
            ps.setString(3, f.arrivalAirport());
            ps.setTime(4,   Time.valueOf(f.departureTime()));
            ps.setTime(5,   Time.valueOf(f.arrivalTime()));
            ps.setString(6, f.operatingDays());
            ps.setBoolean(7, f.isDomestic());
            ps.setBigDecimal(8, f.basePrice());
            ps.setString(9, f.airlineID());
            ps.setString(10, f.flightNumber());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update flight failed: " + f.airlineID() + f.flightNumber(), e);
        }
    }

    public static int delete(String airlineID, String flightNumber) {
        String sql = "DELETE FROM Flight WHERE airlineID = ? AND flightNumber = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete flight failed: " + airlineID + flightNumber, e);
        }
    }

    public static int countTicketsOn(String airlineID, String flightNumber) {
        String sql = "SELECT COUNT(*) AS cnt FROM TicketFlight WHERE airlineID = ? AND flightNumber = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countTicketsOn failed: " + airlineID + flightNumber, e);
        }
    }

    /**
     * All flights touching the given airport, in either direction. UNION ALL
     * of two SELECTs — one for departureAirport=?, one for arrivalAirport=? —
     * with a literal {@code direction} column the UI uses for filtering and
     * grouping.
     */
    public static List<AirportFlightRow> listForAirport(String airportID) {
        String sql = """
                SELECT 'Departing' AS direction,
                       airlineID, flightNumber, arrivalAirport AS otherEndpoint,
                       departureTime, arrivalTime, operatingDays, aircraftID, basePrice
                FROM Flight WHERE departureAirport = ?
                UNION ALL
                SELECT 'Arriving' AS direction,
                       airlineID, flightNumber, departureAirport AS otherEndpoint,
                       departureTime, arrivalTime, operatingDays, aircraftID, basePrice
                FROM Flight WHERE arrivalAirport = ?
                ORDER BY direction ASC, departureTime ASC, airlineID, flightNumber
                """;
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airportID);
            ps.setString(2, airportID);
            try (ResultSet rs = ps.executeQuery()) {
                List<AirportFlightRow> out = new ArrayList<>();
                while (rs.next()) {
                    out.add(new AirportFlightRow(
                            rs.getString("direction"),
                            rs.getString("airlineID"),
                            rs.getString("flightNumber"),
                            rs.getString("otherEndpoint"),
                            rs.getTime("departureTime").toLocalTime(),
                            rs.getTime("arrivalTime").toLocalTime(),
                            rs.getString("operatingDays"),
                            rs.getString("aircraftID"),
                            rs.getBigDecimal("basePrice")));
                }
                return out;
            }
        } catch (SQLException e) {
            throw new RuntimeException("listForAirport failed: " + airportID, e);
        }
    }

    public static int countWaitlistOn(String airlineID, String flightNumber) {
        String sql = "SELECT COUNT(*) AS cnt FROM WaitlistEntry WHERE airlineID = ? AND flightNumber = ?";
        try (Connection c  = Db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, airlineID);
            ps.setString(2, flightNumber);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("cnt");
            }
        } catch (SQLException e) {
            throw new RuntimeException("countWaitlistOn failed: " + airlineID + flightNumber, e);
        }
    }

    private static void bindFlight(PreparedStatement ps, Flight f) throws SQLException {
        ps.setString(1,  f.airlineID());
        ps.setString(2,  f.flightNumber());
        ps.setString(3,  f.aircraftID());
        ps.setString(4,  f.departureAirport());
        ps.setString(5,  f.arrivalAirport());
        ps.setTime(6,    Time.valueOf(f.departureTime()));
        ps.setTime(7,    Time.valueOf(f.arrivalTime()));
        ps.setString(8,  f.operatingDays());
        ps.setBoolean(9, f.isDomestic());
        ps.setBigDecimal(10, f.basePrice());
    }

    private static Flight fromAdminRow(ResultSet rs) throws SQLException {
        return new Flight(
                rs.getString("airlineID"),
                rs.getString("flightNumber"),
                rs.getString("aircraftID"),
                rs.getString("departureAirport"),
                rs.getString("arrivalAirport"),
                rs.getTime("departureTime").toLocalTime(),
                rs.getTime("arrivalTime").toLocalTime(),
                rs.getString("operatingDays"),
                rs.getBoolean("isDomestic"),
                rs.getBigDecimal("basePrice"));
    }

    /** Marker for duplicate composite-PK violations on Flight insert. */
    public static final class DuplicateKeyException extends RuntimeException {
        public DuplicateKeyException(String m) { super(m); }
    }

    private record FlightRow(
            String airlineID, String airlineName, String flightNumber,
            String departureAirport, String arrivalAirport,
            LocalTime departureTime, LocalTime arrivalTime,
            BigDecimal basePrice, String operatingDays) {

        FlightSearchResult toResult(LocalDate date, TravelClass cls) {
            return new FlightSearchResult(
                    date, airlineID, airlineName, flightNumber,
                    departureAirport, arrivalAirport,
                    departureTime, arrivalTime,
                    computeDuration(departureTime, arrivalTime),
                    STOPS_DIRECT,
                    cls,
                    basePrice,
                    PricingService.priceFor(basePrice, cls));
        }
    }
}
