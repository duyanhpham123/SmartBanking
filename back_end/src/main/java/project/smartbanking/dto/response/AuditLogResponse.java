package project.smartbanking.dto.response;

import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.JsonNode;

public record AuditLogResponse(
        Long id,
        Long userId,
        String action,
        String entityType,
        String entityId,
        String ipAddress,
        String userAgent,
        String requestMethod,
        String requestUri,
        JsonNode requestBody,
        Integer responseStatus,
        String description,
        LocalDateTime createdAt) {
}