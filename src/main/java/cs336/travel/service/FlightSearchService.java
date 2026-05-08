package cs336.travel.service;

import cs336.travel.dao.FlightDAO;
import cs336.travel.model.FlightSearchResult;
import cs336.travel.model.RoundTripResult;
import cs336.travel.model.TravelClass;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class FlightSearchService {

    /** Spec: flexible-date searches span centerDate ± this many days. */
    public static final int FLEX_PLUS_MINUS_DAYS = 3;

    private FlightSearchService() {}

    public static List<FlightSearchResult> searchOneWay(
            String fromAirport, String toAirport, LocalDate date,
            boolean flexible, TravelClass cls) {
        validateAirports(fromAirport, toAirport);
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(cls,  "travelClass");
        return flexible
                ? FlightDAO.searchOneWayFlexible(fromAirport, toAirport, date, FLEX_PLUS_MINUS_DAYS, cls)
                : FlightDAO.searchOneWay(fromAirport, toAirport, date, cls);
    }

    public static RoundTripResult searchRoundTrip(
            String fromAirport, String toAirport,
            LocalDate outboundDate, LocalDate returnDate,
            boolean flexible, TravelClass cls) {
        validateAirports(fromAirport, toAirport);
        Objects.requireNonNull(outboundDate, "outboundDate");
        Objects.requireNonNull(returnDate,   "returnDate");
        Objects.requireNonNull(cls,          "travelClass");
        if (returnDate.isBefore(outboundDate)) {
            throw new IllegalArgumentException("returnDate must be on or after outboundDate");
        }
        List<FlightSearchResult> outbound = searchOneWay(fromAirport, toAirport, outboundDate, flexible, cls);
        List<FlightSearchResult> ret      = searchOneWay(toAirport, fromAirport, returnDate,   flexible, cls);
        return new RoundTripResult(outbound, ret);
    }

    private static void validateAirports(String from, String to) {
        Objects.requireNonNull(from, "fromAirport");
        Objects.requireNonNull(to,   "toAirport");
        if (from.isBlank() || to.isBlank()) {
            throw new IllegalArgumentException("from/to airport must be non-blank");
        }
        if (from.equals(to)) {
            throw new IllegalArgumentException("from and to airports must differ");
        }
    }
}
