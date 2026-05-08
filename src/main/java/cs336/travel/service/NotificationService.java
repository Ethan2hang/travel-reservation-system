package cs336.travel.service;

import cs336.travel.Session;
import cs336.travel.dao.NotificationDAO;
import cs336.travel.model.NotificationItem;
import cs336.travel.model.Role;

import java.util.List;

public final class NotificationService {

    private NotificationService() {}

    public static List<NotificationItem> listUnread() {
        if (Session.role() != Role.CUSTOMER) {
            throw new IllegalStateException(
                    "listUnread requires CUSTOMER role; got " + Session.role());
        }
        return NotificationDAO.listUnreadFor(Session.customer().customerID());
    }

    public static int markAllRead() {
        if (Session.role() != Role.CUSTOMER) {
            throw new IllegalStateException(
                    "markAllRead requires CUSTOMER role; got " + Session.role());
        }
        return NotificationDAO.markAllRead(Session.customer().customerID());
    }
}
