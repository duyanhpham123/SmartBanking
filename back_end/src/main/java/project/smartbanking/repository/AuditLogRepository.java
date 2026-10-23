package project.smartbanking.repository;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.AuditLog;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // ===== Admin: danh sách nhật ký (lọc + phân trang) =====
    // userId, action, entityType, keyword có thể null = không lọc.
    // keyword truyền vào dạng đã xử lý sẵn: "%abc%" (chữ thường).
    @Query("""
            select a from AuditLog a
            where (:userId is null or a.userId = :userId)
              and (:action is null or a.action = :action)
              and (:entityType is null or a.entityType = :entityType)
              and a.createdAt >= :fromDate
              and a.createdAt < :toDate
              and (:keyword is null
                   or lower(a.description) like :keyword
                   or lower(a.requestUri) like :keyword)
            """)
    Page<AuditLog> search(@Param("userId") Long userId,
                          @Param("action") String action,
                          @Param("entityType") String entityType,
                          @Param("fromDate") LocalDateTime fromDate,
                          @Param("toDate") LocalDateTime toDate,
                          @Param("keyword") String keyword,
                          Pageable pageable);

    // ===== Nhật ký theo một người dùng (admin xem hoạt động của 1 user) =====
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    // ===== Nhật ký theo một đối tượng, ví dụ TRANSACTION + "1" =====
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, String entityId, Pageable pageable);
}