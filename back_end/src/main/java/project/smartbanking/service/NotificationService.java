package project.smartbanking.service;

import java.math.BigDecimal;
import java.util.Locale;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.NotificationResponse;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.UnreadCountResponse;
import project.smartbanking.event.TransactionCompletedEvent;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.NotificationMapper;
import project.smartbanking.model.Notification;
import project.smartbanking.model.enums.NotificationType;
import project.smartbanking.repository.NotificationRepository;
import project.smartbanking.repository.TransactionRepository;
import project.smartbanking.repository.UserRepository;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationMapper notificationMapper;

    public NotificationService(NotificationRepository notificationRepository,
                               UserRepository userRepository,
                               TransactionRepository transactionRepository,
                               NotificationMapper notificationMapper) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
        this.notificationMapper = notificationMapper;
    }

    // ===================== DANH SÁCH THÔNG BÁO =====================
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getMyNotifications(Long userId, Boolean read,
                                                                 NotificationType type,
                                                                 int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 10 : Math.min(size, 100);
        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<Notification> result;
        if (read != null && type != null) {
            result = notificationRepository.findByUserIdAndReadAndNotificationType(userId, read, type, pageable);
        } else if (read != null) {
            result = notificationRepository.findByUserIdAndRead(userId, read, pageable);
        } else if (type != null) {
            result = notificationRepository.findByUserIdAndNotificationType(userId, type, pageable);
        } else {
            result = notificationRepository.findByUserId(userId, pageable);
        }

        return PageResponse.from(result.map(notificationMapper::toResponse));
    }

    // ===================== SỐ THÔNG BÁO CHƯA ĐỌC =====================
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countByUserIdAndReadFalse(userId));
    }

    // ===================== ĐÁNH DẤU ĐÃ ĐỌC =====================
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        // Thông báo của người khác cũng báo 404
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông báo"));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationRepository.save(notification);
        }
        return notificationMapper.toResponse(notification);
    }

    @Transactional
    public MessageResponse markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsReadByUserId(userId);
        return new MessageResponse("Đã đánh dấu " + updated + " thông báo là đã đọc");
    }

    // ===================== TẠO THÔNG BÁO GIAO DỊCH =====================
    // REQUIRES_NEW: được gọi sau khi giao dịch chính đã commit, nên cần transaction mới để ghi vào database
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyTransaction(TransactionCompletedEvent e) {
        String amount = formatAmount(e.amount(), e.currency());

        switch (e.type()) {
            case DEPOSIT -> create(e.toUserId(), e.transactionId(), "Nạp tiền thành công",
                    "Bạn vừa nạp " + amount + " vào tài khoản " + mask(e.toAccountNumber()) + ".");

            case WITHDRAW -> create(e.fromUserId(), e.transactionId(), "Rút tiền thành công",
                    "Bạn vừa rút " + amount + " từ tài khoản " + mask(e.fromAccountNumber()) + ".");

            case TRANSFER -> {
                String content = (e.description() == null || e.description().isBlank())
                        ? "" : " Nội dung: " + e.description();

                create(e.fromUserId(), e.transactionId(), "Chuyển tiền thành công",
                        "Bạn vừa chuyển " + amount + " đến tài khoản " + mask(e.toAccountNumber()) + "." + content);

                // Chuyển giữa hai tài khoản của chính mình thì chỉ cần một thông báo
                if (!e.fromUserId().equals(e.toUserId())) {
                    create(e.toUserId(), e.transactionId(), "Bạn nhận được tiền",
                            "Tài khoản " + mask(e.toAccountNumber()) + " vừa nhận " + amount
                                    + " từ tài khoản " + mask(e.fromAccountNumber()) + "." + content);
                }
            }

            case PAYMENT -> create(e.fromUserId(), e.transactionId(), "Thanh toán thành công",
                    "Bạn vừa thanh toán " + amount + " từ tài khoản " + mask(e.fromAccountNumber())
                            + ". " + e.description());

            default -> {
                // Các loại giao dịch khác (thanh toán, hoàn tiền) làm sau
            }
        }
    }

    // ===================== THÔNG BÁO BẢO MẬT / HỒ SƠ =====================
    // Chạy chung transaction với nghiệp vụ gọi nó (vd: admin duyệt hồ sơ)
    @Transactional
    public void notifySecurity(Long userId, String title, String message) {
        Notification n = new Notification();
        n.setUser(userRepository.getReferenceById(userId));
        n.setTitle(title);
        n.setMessage(message);
        n.setNotificationType(NotificationType.SECURITY);
        n.setRead(false);
        notificationRepository.save(n);
    }

    // ===================== HÀM PHỤ =====================
    private void create(Long userId, Long transactionId, String title, String message) {
        Notification n = new Notification();
        n.setUser(userRepository.getReferenceById(userId));
        n.setTransaction(transactionRepository.getReferenceById(transactionId));
        n.setTitle(title);
        n.setMessage(message);
        n.setNotificationType(NotificationType.TRANSACTION);
        n.setRead(false);
        notificationRepository.save(n);
    }

    // 1234567890123456 -> ****3456
    private String mask(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 4) {
            return "****";
        }
        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

    // VND: 2.000.000 VND (không lẻ); USD: 2,000.00 USD
    private String formatAmount(BigDecimal amount, String currency) {
        if ("VND".equals(currency)) {
            return String.format(Locale.of("vi", "VN"), "%,.0f", amount) + " VND";
        }
        return String.format(Locale.US, "%,.2f", amount) + " " + currency;
    }
}