package cs336.travel.service;

import cs336.travel.Session;
import cs336.travel.dao.InquiryDAO;
import cs336.travel.model.InquiryRow;
import cs336.travel.model.Role;

import java.util.List;

public final class InquiryService {

    private static final int MIN_LEN = 5;
    private static final int MAX_LEN = 2000;

    private InquiryService() {}

    /**
     * Insert a new inquiry. Validates trimmed length is 5–2000; throws
     * {@link IllegalArgumentException} (caught by the panel and displayed
     * inline) otherwise.
     */
    public static int postQuestion(String text) {
        if (Session.role() != Role.CUSTOMER) {
            throw new IllegalStateException(
                    "postQuestion requires CUSTOMER role; got " + Session.role());
        }
        String trimmed = text == null ? "" : text.trim();
        if (trimmed.length() < MIN_LEN || trimmed.length() > MAX_LEN) {
            throw new IllegalArgumentException(
                    "Question must be " + MIN_LEN + "–" + MAX_LEN + " characters.");
        }
        return InquiryDAO.insertOpen(Session.customer().customerID(), trimmed);
    }

    public static List<InquiryRow> listMyInquiries() {
        if (Session.role() != Role.CUSTOMER) {
            throw new IllegalStateException(
                    "listMyInquiries requires CUSTOMER role; got " + Session.role());
        }
        return InquiryDAO.listForCustomer(Session.customer().customerID());
    }
}
