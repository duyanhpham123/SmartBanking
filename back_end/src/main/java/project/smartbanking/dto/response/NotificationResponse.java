package project.smartbanking.dto.response;

import java.time.LocalDateTime;

import project.smartbanking.model.enums.NotificationType;

public record NotificationResponse(
        Long id,
        String title,
        String message,
        NotificationType notificationType,
        boolean read,
        Long transactionId,
        LocalDateTime createdAt) {
}