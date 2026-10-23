package project.smartbanking.event;

import java.math.BigDecimal;

import project.smartbanking.model.enums.TransactionType;

// Chỉ chứa dữ liệu đơn giản (không có entity) để dùng an toàn sau khi transaction đã kết thúc
public record TransactionCompletedEvent(
        Long transactionId,
        TransactionType type,
        Long fromUserId,
        Long toUserId,
        String fromAccountNumber,
        String toAccountNumber,
        BigDecimal amount,
        String currency,
        String description) {
}