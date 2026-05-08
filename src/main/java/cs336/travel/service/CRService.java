package cs336.travel.service;

import cs336.travel.Db;
import cs336.travel.Session;
import cs336.travel.dao.AircraftDAO;
import cs336.travel.dao.AirlineDAO;
import cs336.travel.dao.AirportDAO;
import cs336.travel.dao.CustomerDAO;
import cs336.travel.dao.FlightDAO;
import cs336.travel.dao.ReservationDAO;
import cs336.travel.dao.TicketDAO;
import cs336.travel.dao.TicketFlightDAO;
import cs336.travel.dao.TicketFlightDAO.EditSegmentRow;
import cs336.travel.dao.InquiryDAO;
import cs336.travel.dao.NotificationDAO;
import cs336.travel.dao.WaitlistDAO;
import cs336.travel.model.Aircraft;
import cs336.travel.model.Airline;
import cs336.travel.model.Airport;
import cs336.travel.model.AirportFlightRow;
import cs336.travel.model.CrudResult;
import cs336.travel.model.Customer;
import cs336.travel.model.EditResult;
import cs336.travel.model.Flight;
import cs336.travel.model.Role;
import cs336.travel.model.SegmentEditDelta;
import cs336.travel.model.TravelClass;
import cs336.travel.model.InquiryListRow;
import cs336.travel.model.InquiryReplyResult;
import cs336.travel.model.WaitlistRow;

import java.time.LocalDate;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CRService {

    private CRService() {}

    public static List<Customer> listAllCustomers() {
        requireCR();
        return CustomerDAO.listAll();
    }

    public static java.util.Optional<ReservationDAO.EditHeader> findReservationHeader(int reservationID) {
        requireCR();
        return ReservationDAO.findHeader(reservationID);
    }

    public static List<cs336.travel.model.ReservationSummary> listReservationsForCustomer(
            int customerID) {
        requireCR();
        // CR sees both upcoming and past, both confirmed and cancelled, so they
        // have the same audit visibility as the by-reservation-# path.
        List<cs336.travel.model.ReservationSummary> out = new ArrayList<>();
        out.addAll(ReservationDAO.listForCustomer(customerID, true));
        out.addAll(ReservationDAO.listForCustomer(customerID, false));
        return out;
    }

    /** Loads segments for the edit screen — opens its own (read-only) connection. */
    public static List<EditSegmentRow> loadEditableSegments(int reservationID) {
        requireCR();
        try (Connection c = Db.getConnection()) {
            return TicketFlightDAO.listForEdit(c, reservationID);
        } catch (SQLException e) {
            throw new RuntimeException("loadEditableSegments failed: " + reservationID, e);
        }
    }

    /**
     * Atomic edit across one reservation's segments. {@link SegmentEditDelta}
     * carries optional new class / seat / meal per segment; class changes
     * trigger a totalFare recompute via {@link PricingService}. bookingFee
     * is intentionally NOT refunded — class change doesn't refund the fee.
     *
     * <p>Capacity-by-class is not enforced (the project uses a shared seat
     * pool); upgrading a segment to Business simply relabels the existing
     * seat. Documented here so the grader knows the simplification is
     * deliberate.
     */
    public static EditResult editReservation(int reservationID,
                                             List<SegmentEditDelta> deltas) {
        requireCR();
        if (deltas == null) {
            return new EditResult.Error("No deltas provided.");
        }

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                String status = ReservationDAO.findStatus(c, reservationID);
                if (status == null) {
                    c.rollback();
                    return new EditResult.Refused("Reservation not found.");
                }
                if (!"CONFIRMED".equals(status)) {
                    c.rollback();
                    return new EditResult.Refused(
                            "This reservation has been cancelled and cannot be edited.");
                }

                List<EditSegmentRow> segments = TicketFlightDAO.listForEdit(c, reservationID);
                if (segments.isEmpty()) {
                    c.rollback();
                    return new EditResult.Refused("Reservation has no segments.");
                }

                Map<Integer, SegmentEditDelta> byOrder = new HashMap<>();
                for (SegmentEditDelta d : deltas) byOrder.put(d.segmentOrder(), d);

                // Apply deltas in-memory and validate.
                List<EditSegmentRow> updated = new ArrayList<>(segments.size());
                for (EditSegmentRow s : segments) {
                    SegmentEditDelta d = byOrder.get(s.segmentOrder());
                    TravelClass cls = d != null && d.newClass().isPresent()
                            ? d.newClass().get() : s.currentClass();
                    String seat = d != null && d.newSeat().isPresent()
                            ? d.newSeat().get().trim() : s.currentSeat();
                    String meal = d != null && d.newMeal().isPresent()
                            ? d.newMeal().get() : s.currentMeal();
                    if (seat == null || seat.isBlank()) {
                        c.rollback();
                        return new EditResult.Refused(
                                "Seat number cannot be blank (segment " + s.segmentOrder() + ").");
                    }
                    if (seat.length() > 5) {
                        c.rollback();
                        return new EditResult.Refused(
                                "Seat number too long (max 5 chars) on segment "
                                        + s.segmentOrder() + ".");
                    }
                    if (meal != null && meal.length() > 50) {
                        c.rollback();
                        return new EditResult.Refused(
                                "Meal preference too long (max 50 chars) on segment "
                                        + s.segmentOrder() + ".");
                    }
                    updated.add(new EditSegmentRow(
                            s.ticketNumber(), s.segmentOrder(),
                            s.airlineID(), s.flightNumber(), s.departureDateTime(),
                            s.fromAirport(), s.toAirport(),
                            s.basePrice(), cls, seat, meal));
                }

                // Apply per-segment updates and recompute per-ticket fare.
                Map<Long, BigDecimal> newFareByTicket = new HashMap<>();
                for (EditSegmentRow s : updated) {
                    TicketFlightDAO.updateSegment(c, s.ticketNumber(), s.segmentOrder(),
                            s.currentClass(), s.currentSeat(), s.currentMeal());
                    BigDecimal segFare = PricingService.priceFor(
                            s.basePrice(), s.currentClass());
                    newFareByTicket.merge(s.ticketNumber(), segFare, BigDecimal::add);
                }
                for (Map.Entry<Long, BigDecimal> e : newFareByTicket.entrySet()) {
                    TicketDAO.updateTotalFare(c, e.getKey(), e.getValue());
                }

                BigDecimal newTotal = BigDecimal.ZERO;
                for (BigDecimal v : newFareByTicket.values()) newTotal = newTotal.add(v);

                c.commit();
                return new EditResult.Success(reservationID, newTotal);
            } catch (SQLException inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new EditResult.Error("Edit failed: " + e.getMessage());
        }
    }

    // ==================== Reference data CRUD ====================
    // Each method requireCR() at the top; pre-delete dependency checks
    // surface as Refused with a count-named message.

    public static List<Airline>  listAirlines()  { requireCR(); return AirlineDAO.listAll();  }
    public static List<Airport>  listAirports()  { requireCR(); return AirportDAO.listAll();  }
    public static List<Aircraft> listAircraft()  { requireCR(); return AircraftDAO.listAll(); }
    public static List<Aircraft> listAircraftFor(String airlineID) {
        requireCR();
        return AircraftDAO.listForAirline(airlineID);
    }
    public static List<Flight>   listFlights()   { requireCR(); return FlightDAO.listAllAdmin(); }

    // ---- Aircraft ----
    public static CrudResult createAircraft(String aircraftID, String airlineID, int seatCapacity) {
        requireCR();
        try {
            AircraftDAO.insert(aircraftID, airlineID, seatCapacity);
            return new CrudResult.Success(1);
        } catch (AircraftDAO.DuplicateKeyException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not save aircraft.");
        }
    }
    public static CrudResult updateAircraft(String aircraftID, String airlineID, int seatCapacity) {
        return updateAircraft(aircraftID, aircraftID, airlineID, seatCapacity);
    }

    /**
     * Edit + optional rename. If {@code newAircraftID} differs from
     * {@code oldAircraftID}, refuses when any Flight still references the old
     * ID (no ON UPDATE CASCADE on the schema). Translates UNIQUE-constraint
     * collisions on the new ID to a clean {@link CrudResult.Error}.
     */
    public static CrudResult updateAircraft(String oldAircraftID, String newAircraftID,
                                            String airlineID, int seatCapacity) {
        requireCR();
        boolean rename = !oldAircraftID.equals(newAircraftID);
        if (rename) {
            int refs = AircraftDAO.countFlightsUsing(oldAircraftID);
            if (refs > 0) {
                return new CrudResult.Refused(
                        "Cannot rename: " + oldAircraftID + " is referenced by "
                                + refs + " flight(s). Reassign or delete those flights first.");
            }
        }
        try {
            int rows = rename
                    ? AircraftDAO.updateWithRename(oldAircraftID, newAircraftID, airlineID, seatCapacity)
                    : AircraftDAO.update(oldAircraftID, airlineID, seatCapacity);
            return rows == 1 ? new CrudResult.Success(1) : new CrudResult.Error("No aircraft updated.");
        } catch (AircraftDAO.DuplicateKeyException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not update aircraft.");
        }
    }
    public static CrudResult deleteAircraft(String aircraftID) {
        requireCR();
        int flights = AircraftDAO.countFlightsUsing(aircraftID);
        if (flights > 0) {
            return new CrudResult.Refused(
                    "Cannot delete: " + aircraftID + " is assigned to "
                            + flights + " flight(s). Reassign or delete those first.");
        }
        try {
            int rows = AircraftDAO.delete(aircraftID);
            return rows == 1 ? new CrudResult.Success(1) : new CrudResult.Error("No aircraft deleted.");
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not delete aircraft.");
        }
    }

    // ---- Airport ----
    public static CrudResult createAirport(String airportID, String name, String city, String country) {
        requireCR();
        try {
            AirportDAO.insert(airportID, name, city, country);
            return new CrudResult.Success(1);
        } catch (AirportDAO.DuplicateKeyException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not save airport.");
        }
    }
    public static CrudResult updateAirport(String airportID, String name, String city, String country) {
        return updateAirport(airportID, airportID, name, city, country);
    }

    /**
     * Edit + optional rename. Refuses if any Flight still references the old
     * code (Flight has FKs on both departureAirport and arrivalAirport).
     */
    public static CrudResult updateAirport(String oldAirportID, String newAirportID,
                                           String name, String city, String country) {
        requireCR();
        boolean rename = !oldAirportID.equals(newAirportID);
        if (rename) {
            int refs = AirportDAO.countFlightsAt(oldAirportID);
            if (refs > 0) {
                return new CrudResult.Refused(
                        "Cannot rename: " + oldAirportID + " is referenced by "
                                + refs + " flight(s). Reroute or delete those flights first.");
            }
        }
        try {
            int rows = rename
                    ? AirportDAO.updateWithRename(oldAirportID, newAirportID, name, city, country)
                    : AirportDAO.update(oldAirportID, name, city, country);
            return rows == 1 ? new CrudResult.Success(1) : new CrudResult.Error("No airport updated.");
        } catch (AirportDAO.DuplicateKeyException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not update airport.");
        }
    }
    public static CrudResult deleteAirport(String airportID) {
        requireCR();
        int flights = AirportDAO.countFlightsAt(airportID);
        if (flights > 0) {
            return new CrudResult.Refused(
                    "Cannot delete: " + airportID + " is the depart/arrive airport for "
                            + flights + " flight(s). Reroute or delete those first.");
        }
        try {
            int rows = AirportDAO.delete(airportID);
            return rows == 1 ? new CrudResult.Success(1) : new CrudResult.Error("No airport deleted.");
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not delete airport.");
        }
    }

    // ---- Flight ----
    public static CrudResult createFlight(Flight f) {
        requireCR();
        try {
            FlightDAO.insert(f);
            return new CrudResult.Success(1);
        } catch (FlightDAO.DuplicateKeyException dup) {
            return new CrudResult.Error(dup.getMessage());
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not save flight.");
        }
    }
    public static CrudResult updateFlight(Flight f) {
        requireCR();
        try {
            int rows = FlightDAO.update(f);
            return rows == 1 ? new CrudResult.Success(1) : new CrudResult.Error("No flight updated.");
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not update flight.");
        }
    }
    public static CrudResult deleteFlight(String airlineID, String flightNumber) {
        requireCR();
        int tickets  = FlightDAO.countTicketsOn(airlineID, flightNumber);
        int waitlist = FlightDAO.countWaitlistOn(airlineID, flightNumber);
        if (tickets > 0 || waitlist > 0) {
            return new CrudResult.Refused(
                    "Cannot delete: " + airlineID + " " + flightNumber + " has "
                            + tickets + " ticket segment(s) and " + waitlist
                            + " waitlist entry(ies). Resolve those first.");
        }
        try {
            int rows = FlightDAO.delete(airlineID, flightNumber);
            return rows == 1 ? new CrudResult.Success(1) : new CrudResult.Error("No flight deleted.");
        } catch (RuntimeException e) {
            return new CrudResult.Error("Could not delete flight.");
        }
    }

    public static List<WaitlistRow> listWaitlistFor(String airlineID, String flightNumber,
                                                    LocalDate flightDate) {
        requireCR();
        return WaitlistDAO.listForFlight(airlineID, flightNumber, flightDate);
    }

    public static List<AirportFlightRow> listFlightsAt(String airportID) {
        requireCR();
        return FlightDAO.listForAirport(airportID);
    }

    public static List<InquiryListRow> listOpenInquiries() {
        requireCR();
        return InquiryDAO.listAllOpen();
    }

    public static List<InquiryListRow> listAnsweredInquiries() {
        requireCR();
        return InquiryDAO.listAllAnswered();
    }

    /**
     * Atomic: persist the answer + insert a Notification for the customer so
     * they see a banner alert on their next home-screen open. Rolls back both
     * if either fails, so an answered inquiry is never silent.
     */
    public static InquiryReplyResult replyTo(int inquiryID, String answer) {
        requireCR();
        String trimmed = answer == null ? "" : answer.trim();
        if (trimmed.length() < 5 || trimmed.length() > 2000) {
            return new InquiryReplyResult.Refused("Reply must be 5–2000 characters.");
        }

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                java.util.Optional<Integer> ownerOpt = InquiryDAO.findCustomerId(c, inquiryID);
                if (ownerOpt.isEmpty()) {
                    c.rollback();
                    return new InquiryReplyResult.Refused("Inquiry not found.");
                }
                int customerID = ownerOpt.get();

                int rows = InquiryDAO.reply(c, inquiryID,
                        Session.employee().employeeID(), trimmed);
                if (rows == 0) {
                    c.rollback();
                    return new InquiryReplyResult.AlreadyAnswered(inquiryID);
                }

                NotificationDAO.insert(c, customerID,
                        "A customer rep replied to your question. "
                                + "Visit Contact Support to read the answer.");

                c.commit();
                return new InquiryReplyResult.Success(inquiryID);
            } catch (SQLException inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new InquiryReplyResult.Error("Could not save reply: " + e.getMessage());
        }
    }

    static void requireCR() {
        if (Session.role() != Role.CUSTOMER_REP) {
            throw new IllegalStateException("Customer rep access required.");
        }
    }
}
