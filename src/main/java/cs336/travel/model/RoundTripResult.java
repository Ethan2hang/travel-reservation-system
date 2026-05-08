package cs336.travel.model;

import java.util.List;

/**
 * Pair of one-way searches forming a round-trip query. Either leg may be
 * empty independently — a customer can have outbound matches with no
 * return option, or vice versa.
 */
public record RoundTripResult(
        List<FlightSearchResult> outbound,
        List<FlightSearchResult> ret) {
}
