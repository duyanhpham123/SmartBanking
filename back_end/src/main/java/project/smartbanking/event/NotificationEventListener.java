package project.smartbanking.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import project.smartbanking.service.NotificationService;

@Component
public class NotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventListener.class);

    private final NotificationService notificationService;

    public NotificationEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // Chỉ chạy khi giao dịch đã commit thành công
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onTransactionCompleted(TransactionCompletedEvent event) {
        try {
            notificationService.notifyTransaction(event);
        } catch (Exception ex) {
            // Lỗi thông báo không được làm hỏng giao dịch đã hoàn tất
            log.error("Không tạo được thông báo cho giao dịch {}", event.transactionId(), ex);
        }
    }
}