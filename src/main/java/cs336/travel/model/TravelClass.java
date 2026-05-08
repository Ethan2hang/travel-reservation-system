package cs336.travel.model;

/**
 * Mirrors {@code TicketFlight.class} ENUM in {@code db/schema.sql}.
 * Stored as the same string at the JDBC boundary.
 */
public enum TravelClass {
    ECONOMY,
    BUSINESS,
    FIRST;

    /** Display label shown in the UI table's "Class" column. */
    public String label() {
        return switch (this) {
            case ECONOMY  -> "Economy";
            case BUSINESS -> "Business";
            case FIRST    -> "First";
        };
    }
}
