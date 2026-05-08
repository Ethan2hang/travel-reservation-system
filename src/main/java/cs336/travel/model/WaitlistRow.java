package cs336.travel.model;

import java.time.LocalDateTime;

public record WaitlistRow(
        int waitlistID,
        String username,
        String customerName,
        String email,
        String phone,
        TravelClass travelClass,
        String status,
        LocalDateTime requestedAt) {
}
