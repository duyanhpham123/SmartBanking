package project.smartbanking.mapper;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import project.smartbanking.dto.response.AuditLogResponse;
import project.smartbanking.model.AuditLog;

@Component
public class AuditLogMapper {

    private final ObjectMapper objectMapper;

    public AuditLogMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public AuditLogResponse toResponse(AuditLog a) {
        return new AuditLogResponse(
                a.getId(),
                a.getUserId(),
                a.getAction(),
                a.getEntityType(),
                a.getEntityId(),
                a.getIpAddress(),
                a.getUserAgent(),
                a.getRequestMethod(),
                a.getRequestUri(),
                parseJson(a.getRequestBody()),
                a.getResponseStatus(),
                a.getDescription(),
                a.getCreatedAt());
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}