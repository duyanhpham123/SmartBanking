package project.smartbanking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.response.AuditLogResponse;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.AuditLogMapper;
import project.smartbanking.model.AuditLog;
import project.smartbanking.repository.AuditLogRepository;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogService(AuditLogRepository auditLogRepository, AuditLogMapper auditLogMapper) {
        this.auditLogRepository = auditLogRepository;
        this.auditLogMapper = auditLogMapper;
    }

    // ===================== GHI NHẬT KÝ =====================
    // REQUIRES_NEW: ghi trong transaction riêng, không phụ thuộc vào việc nghiệp vụ chính thành công hay rollback
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(Long userId, String action, String entityType, String entityId,
                       String ipAddress, String userAgent, String requestMethod, String requestUri,
                       String requestBodyJson, Integer responseStatus, String description) {
        AuditLog entry = new AuditLog();
        entry.setUserId(userId);
        entry.setAction(cut(action, 100));
        entry.setEntityType(cut(entityType, 100));
        entry.setEntityId(cut(entityId, 100));
        entry.setIpAddress(cut(ipAddress, 45));
        entry.setUserAgent(cut(userAgent, 500));
        entry.setRequestMethod(cut(requestMethod, 10));
        entry.setRequestUri(cut(requestUri, 500));
        entry.setRequestBody(requestBodyJson);
        entry.setResponseStatus(responseStatus);
        entry.setDescription(cut(description, 1000));
        auditLogRepository.save(entry);
    }

    // ===================== XEM NHẬT KÝ (ADMIN) =====================
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> search(Long userId, String action, String entityType,
                                                 LocalDate from, LocalDate to, String keyword,
                                                 int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        LocalDateTime fromDate = from == null
                ? LocalDateTime.of(1970, 1, 1, 0, 0)
                : from.atStartOfDay();
        // Điều kiện trong query là "< toDate", nên lấy đầu ngày hôm sau để gồm cả ngày cuối
        LocalDateTime toDate = to == null
                ? LocalDate.now().plusDays(1).atStartOfDay()
                : to.plusDays(1).atStartOfDay();

        String act = (action == null || action.isBlank()) ? null : action.trim().toUpperCase();
        String type = (entityType == null || entityType.isBlank()) ? null : entityType.trim().toUpperCase();
        String kw = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim().toLowerCase() + "%";

        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<AuditLog> result = auditLogRepository.search(userId, act, type, fromDate, toDate, kw, pageable);
        return PageResponse.from(result.map(auditLogMapper::toResponse));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public AuditLogResponse getById(Long id) {
        return auditLogRepository.findById(id)
                .map(auditLogMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhật ký"));
    }

    private String cut(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}