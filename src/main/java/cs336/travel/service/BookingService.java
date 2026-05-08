package cs336.travel.service;

import cs336.travel.Db;
import cs336.travel.Session;
import cs336.travel.dao.AircraftDAO;
import cs336.travel.dao.ReservationDAO;
import cs336.travel.dao.TicketDAO;
import cs336.travel.dao.TicketFlightDAO;
import cs336.travel.model.BookingResult;
import cs336.travel.model.Role;
import cs336.travel.model.SelectedSegment;
import cs336.travel.model.TravelClass;
import cs336.travel.model.TripType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

public final class BookingService {

    /** The company's revenue per ticket, regardless of class or segment count. */
    public static final BigDecimal BOOKING_FEE = new BigDecimal("25.00");

    private BookingService() {}

    /**
     * Book a Reservation + Ticket + N TicketFlight rows in a single transaction.
     *
     * <p>Capacity is checked per segment before any insert; if any leg is full
     * the entire booking is rejected (no partial state).
     *
     * <p>Seats are auto-assigned sequentially per flight instance — no
     * seat-picker UI in this feature; if needed for the demo it's a 5-line
     * addition to the panel.
     *
     * @param customerID            the customer the reservation is for
     * @param createdByEmployeeID   null for self-booking; set when a CR books on behalf
     * @param tripType              ONE_WAY (1 segment) or ROUND_TRIP (2 segments)
     * @param cls                   class of service for every segment
     * @param segments              ordered list of legs
     */
    public static BookingResult book(int customerID,
                                     Integer createdByEmployeeID,
                                     TripType tripType,
                                     TravelClass cls,
                                     List<SelectedSegment> segments) {
        Objects.requireNonNull(tripType, "tripType");
        Objects.requireNonNull(cls,      "travelClass");
        Objects.requireNonNull(segments, "segments");
        if (segments.isEmpty()) {
            return new BookingResult.Error("No flights selected.");
        }
        if (tripType == TripType.ONE_WAY    && segments.size() != 1) {
            return new BookingResult.Error("One-way requires exactly 1 segment.");
        }
        if (tripType == TripType.ROUND_TRIP && segments.size() != 2) {
            return new BookingResult.Error("Round-trip requires exactly 2 segments.");
        }

        Role role = Session.role();
        if (role == null) {
            return new BookingResult.Error("Not signed in.");
        }
        switch (role) {
            case CUSTOMER -> {
                if (createdByEmployeeID != null) {
                    return new BookingResult.Error("Customer self-booking cannot set createdByEmployeeID.");
                }
                if (Session.customer() == null
                        || Session.customer().customerID() != customerID) {
                    return new BookingResult.Error("Customer can only book for themselves.");
                }
            }
            case CUSTOMER_REP -> {
                if (Session.employee() == null) {
                    return new BookingResult.Error("Customer rep session missing.");
                }
                if (createdByEmployeeID == null
                        || createdByEmployeeID != Session.employee().employeeID()) {
                    return new BookingResult.Error(
                            "Customer rep must book with their own employeeID.");
                }
            }
            case ADMIN -> throw new IllegalStateException("Admins do not book reservations.");
        }

        BigDecimal totalFare = BigDecimal.ZERO;
        for (SelectedSegment s : segments) {
            totalFare = totalFare.add(PricingService.priceFor(s.basePrice(), cls));
        }

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                for (SelectedSegment s : segments) {
                    int booked   = TicketFlightDAO.countBookedSeats(
                            c, s.airlineID(), s.flightNumber(), s.departureDateTime());
                    int capacity = AircraftDAO.getCapacityForFlight(
                            c, s.airlineID(), s.flightNumber());
                    if (booked >= capacity) {
                        c.rollback();
                        return new BookingResult.Full(
                                s.airlineID(), s.flightNumber(),
                                s.departureDateTime().toLocalDate());
                    }
                }

                int reservationID = ReservationDAO.insertReservation(
                        c, customerID, createdByEmployeeID);
                long ticketNumber = TicketDAO.insertTicket(
                        c, reservationID, tripType, totalFare, BOOKING_FEE);

                int segOrder = 1;
                for (SelectedSegment s : segments) {
                    int nextSeat = TicketFlightDAO.maxSeatNumber(
                            c, s.airlineID(), s.flightNumber(), s.departureDateTime()) + 1;
                    TicketFlightDAO.insertSegment(
                            c, ticketNumber, segOrder++,
                            s.airlineID(), s.flightNumber(), s.departureDateTime(),
                            String.valueOf(nextSeat), cls);
                }

                c.commit();
                return new BookingResult.Success(reservationID, ticketNumber);
            } catch (SQLException inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new BookingResult.Error("Booking failed: " + e.getMessage());
        }
    }
}
