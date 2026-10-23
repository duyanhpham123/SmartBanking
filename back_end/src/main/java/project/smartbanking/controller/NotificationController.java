package project.smartbanking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.NotificationResponse;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.UnreadCountResponse;
import project.smartbanking.model.enums.NotificationType;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.NotificationService;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Boolean read,
            @RequestParam(required = false) NotificationType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(notificationService.getMyNotifications(
                principal.getId(), read, type, page, size));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> getUnreadCount(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.getUnreadCount(principal.getId()));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<NotificationResponse> markAsRead(@AuthenticationPrincipal UserPrincipal principal,
                                                           @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markAsRead(principal.getId(), id));
    }

    @PutMapping("/read-all")
    public ResponseEntity<MessageResponse> markAllAsRead(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(notificationService.markAllAsRead(principal.getId()));
    }
}