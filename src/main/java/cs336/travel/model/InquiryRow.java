package cs336.travel.model;

import java.time.LocalDateTime;

/**
 * One row from the {@code Inquiry} table for the customer's history view.
 * {@code answer} and {@code answeredAt} are null while status is OPEN; the
 * CR-side reply screen (Block 4) populates them.
 */
public record InquiryRow(
        int inquiryID,
        String question,
        LocalDateTime postedAt,
        String answer,
        LocalDateTime answeredAt,
        String status) {
}
