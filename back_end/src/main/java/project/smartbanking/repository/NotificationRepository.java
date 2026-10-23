package project.smartbanking.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.Notification;
import project.smartbanking.model.enums.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    // ===== Danh sách thông báo của người dùng (phân trang) =====
    Page<Notification> findByUserId(Long userId, Pageable pageable);

    // Lọc theo đã đọc / chưa đọc
    Page<Notification> findByUserIdAndRead(Long userId, boolean read, Pageable pageable);

    // Lọc theo loại (TRANSACTION, SECURITY, SYSTEM, PROMOTION)
    Page<Notification> findByUserIdAndNotificationType(Long userId, NotificationType type, Pageable pageable);
    
    // Lọc đồng thời theo trạng thái đọc và loại thông báo
    Page<Notification> findByUserIdAndReadAndNotificationType(Long userId, boolean read,
                                                              NotificationType type, Pageable pageable);

    // ===== Số thông báo chưa đọc (badge ở chuông + thẻ "Thông báo mới" ở trang chủ) =====
    long countByUserIdAndReadFalse(Long userId);

    // ===== Đánh dấu đã đọc =====
    // Dùng findByIdAndUserId để chắc chắn thông báo thuộc đúng người đang đăng nhập
    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    // Đánh dấu tất cả đã đọc
    @Modifying(clearAutomatically = true)
    @Query("update Notification n set n.read = true where n.user.id = :userId and n.read = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);
}