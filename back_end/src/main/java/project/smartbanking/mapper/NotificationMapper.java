package project.smartbanking.mapper;

import org.springframework.stereotype.Component;

import project.smartbanking.dto.response.NotificationResponse;
import project.smartbanking.model.Notification;

@Component
public class NotificationMapper {

    // Phải gọi bên trong @Transactional vì getTransaction() là LAZY
    public NotificationResponse toResponse(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getTitle(),
                n.getMessage(),
                n.getNotificationType(),
                n.isRead(),
                n.getTransaction() == null ? null : n.getTransaction().getId(),
                n.getCreatedAt());
    }
}