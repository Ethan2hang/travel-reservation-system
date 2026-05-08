package cs336.travel.ui;

/**
 * Display-formatting helpers shared across panels. Keeps user-facing strings
 * consistent and prevents bugs like {@code airlineID + flightNumber} producing
 * "AAAA100".
 */
final class Format {

    private Format() {}

    /** Renders a flight identifier as "AA AA100" (space-separated). */
    static String flightLabel(String airlineID, String flightNumber) {
        return airlineID + " " + flightNumber;
    }
}
