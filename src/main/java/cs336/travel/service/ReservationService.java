package cs336.travel.service;

import cs336.travel.Session;
import cs336.travel.dao.ReservationDAO;
import cs336.travel.dao.TicketFlightDAO;
import cs336.travel.model.ReservationSummary;
import cs336.travel.model.Role;
import cs336.travel.model.SegmentDetail;

import java.util.List;

public final class ReservationService {

    private ReservationService() {}

    /**
     * Returns the signed-in customer's reservations, bucketed by date.
     * The customerID is read from the session — never from a UI argument —
     * so a customer cannot ask to see another's history.
     */
    public static List<ReservationSummary> listMyReservations(boolean upcoming) {
        if (Session.role() != Role.CUSTOMER) {
            throw new IllegalStateException(
                    "listMyReservations requires CUSTOMER role; got " + Session.role());
        }
        return ReservationDAO.listForCustomer(
                Session.customer().customerID(), upcoming);
    }

    /**
     * Returns segment detail for a reservation that belongs to the signed-in
     * customer. Throws if the reservation isn't theirs.
     */
    public static List<SegmentDetail> listSegments(int reservationID) {
        if (Session.role() != Role.CUSTOMER) {
            throw new IllegalStateException(
                    "listSegments requires CUSTOMER role; got " + Session.role());
        }
        if (!ReservationDAO.ownedBy(reservationID, Session.customer().customerID())) {
            throw new IllegalStateException(
                    "Reservation " + reservationID + " does not belong to current customer.");
        }
        return TicketFlightDAO.listForReservation(reservationID);
    }
}
