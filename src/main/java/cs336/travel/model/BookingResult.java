package cs336.travel.model;

/**
 * Outcome of {@code BookingService.book(...)}. Sealed so the UI exhausts all
 * three cases without a default branch.
 */
public sealed interface BookingResult
        permits BookingResult.Success, BookingResult.Full, BookingResult.Error {

    record Success(int reservationID, long ticketNumber) implements BookingResult {}

    /** Capacity exhausted on this flight instance. UI offers waitlist. */
    record Full(String airlineID, String flightNumber, java.time.LocalDate flightDate)
            implements BookingResult {}

    record Error(String message) implements BookingResult {}
}
