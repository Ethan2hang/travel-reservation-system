package cs336.travel.service;

import cs336.travel.Db;
import cs336.travel.Session;
import cs336.travel.dao.WaitlistDAO;
import cs336.travel.model.Role;
import cs336.travel.model.SelectedSegment;
import cs336.travel.model.TravelClass;
import cs336.travel.model.WaitlistResult;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WaitlistService {

    private WaitlistService() {}

    /**
     * Insert one {@code WaitlistEntry} per segment, atomically. Used when
     * {@code BookingService} returns {@code Full}; the user has already been
     * shown the capacity-full dialog and confirmed they want to wait.
     *
     * <p>Position numbers are read inside the transaction so they line up
     * with what the dialog showed (the count cannot have decreased since
     * the dialog opened — entries only leave WAITING via cancellation).
     */
    public static WaitlistResult addToWaitlist(int customerID,
                                               List<SelectedSegment> segments,
                                               TravelClass cls) {
        Objects.requireNonNull(segments, "segments");
        Objects.requireNonNull(cls,      "travelClass");
        if (segments.isEmpty()) {
            return new WaitlistResult.Error("No flights selected.");
        }

        Role role = Session.role();
        if (role != Role.CUSTOMER) {
            throw new IllegalStateException("Only customers add themselves to waitlists.");
        }
        if (Session.customer() == null
                || Session.customer().customerID() != customerID) {
            throw new IllegalStateException("Customer can only waitlist themselves.");
        }

        try (Connection c = Db.getConnection()) {
            c.setAutoCommit(false);
            try {
                List<Integer> ids       = new ArrayList<>(segments.size());
                List<Integer> positions = new ArrayList<>(segments.size());
                for (SelectedSegment s : segments) {
                    int waiting = WaitlistDAO.countWaiting(
                            c, s.airlineID(), s.flightNumber(),
                            s.departureDateTime().toLocalDate());
                    int id = WaitlistDAO.insertEntry(
                            c, customerID, s.airlineID(), s.flightNumber(),
                            s.departureDateTime().toLocalDate(), cls);
                    ids.add(id);
                    positions.add(waiting + 1);
                }
                c.commit();
                return new WaitlistResult.Success(ids, positions);
            } catch (SQLException inner) {
                c.rollback();
                throw inner;
            } finally {
                c.setAutoCommit(true);
            }
        } catch (SQLException e) {
            return new WaitlistResult.Error("Waitlist insert failed: " + e.getMessage());
        }
    }
}
