package cs336.travel.model;

import java.time.LocalDateTime;

/**
 * Inquiry row joined with the customer who posted it (and, when status is
 * ANSWERED, the employee who replied). Powers the CR-side Open/Answered
 * tables.
 */
public record InquiryListRow(
        int inquiryID,
        int customerID,
        String customerUsername,
        String customerName,
        String question,
        LocalDateTime postedAt,
        String answer,
        LocalDateTime answeredAt,
        String answeredByUsername,
        String status) {
}
