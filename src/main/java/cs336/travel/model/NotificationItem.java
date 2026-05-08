package cs336.travel.model;

import java.time.LocalDateTime;

public record NotificationItem(
        int notificationID,
        String message,
        LocalDateTime createdAt) {
}
