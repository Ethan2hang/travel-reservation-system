package cs336.travel.service;

import cs336.travel.Db;
import cs336.travel.Session;
import cs336.travel.dao.NotificationDAO;
import cs336.travel.dao.ReservationDAO;
import cs336.travel.dao.TicketFlightDAO;
import cs336.travel.dao.TicketFlightDAO.CancelSegmentRow;
import cs336.travel.dao.WaitlistDAO;
import cs336.travel.model.CancelResult;
import cs336.travel.model.PromotedEntry;
import cs336.travel.model.Role;
import cs336.travel.model.TravelClass;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class CancelService {

    private CancelService() {}

    /**
     * Cancel a reservation owned by the signed-in customer. Atomic across:
     * status flip, per-segment waitlist promotion, and notification insert.
     * Economy reservations are refused (per ProjectSp2026 §cancel).
     */
    public static CancelResult cancel(int reservationID) {
        if (Session.role() != Role.CUSTOMER) {
            throw new IllegalStateException(
                    "cancel requires CUSTOMER role; got " + Session.role());
        }
        int sessionCustomerID = Session.customer().customerID();

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                Optional<int[]> ownerStatus = ReservationDAO.findOwnerAndStatus(c, reservationID);
                if (ownerStatus.isEmpty()) {
                    c.rollback();
                    return new CancelResult.Refused("Reservation not found.");
                }
                int ownerCustomerID = ownerStatus.get()[0];
                boolean isConfirmed = ownerStatus.get()[1] == 1;
                if (ownerCustomerID != sessionCustomerID) {
                    c.rollback();
                    return new CancelResult.Refused(
                            "You can only cancel your own reservations.");
                }
                if (!isConfirmed) {
                    c.rollback();
                    return new CancelResult.Refused(
                            "Reservation is already cancelled.");
                }

                List<CancelSegmentRow> segments =
                        TicketFlightDAO.listForCancel(c, reservationID);
                for (CancelSegmentRow s : segments) {
                    if (s.cls() == TravelClass.ECONOMY) {
                        c.rollback();
                        return new CancelResult.Refused(
                                "Economy reservations cannot be cancelled per airline policy.");
                    }
                }

                int updated = ReservationDAO.markCancelled(c, reservationID);
                if (updated != 1) {
                    c.rollback();
                    return new CancelResult.Error(
                            "Reservation could not be marked cancelled (concurrent change?).");
                }

                int promoted = 0;
                for (CancelSegmentRow s : segments) {
                    Optional<PromotedEntry> p = WaitlistDAO.promoteFirstWaiting(
                            c, s.airlineID(), s.flightNumber(),
                            s.departureDateTime().toLocalDate());
                    if (p.isPresent()) {
                        PromotedEntry pe = p.get();
                        String message = "A seat opened on "
                                + pe.airlineID() + " " + pe.flightNumber()
                                + " on " + pe.flightDate()
                                + " (" + pe.travelClass().label() + ")."
                                + " Visit Search Flights to book it.";
                        NotificationDAO.insert(c, pe.customerID(), message);
                        promoted++;
                    }
                }

                c.commit();
                return new CancelResult.Success(reservationID, promoted);
            } catch (SQLException inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new CancelResult.Error("Cancel failed: " + e.getMessage());
        }
    }
}
