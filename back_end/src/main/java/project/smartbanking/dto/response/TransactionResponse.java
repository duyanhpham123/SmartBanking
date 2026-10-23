package project.smartbanking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import project.smartbanking.model.enums.TransactionDirection;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;

public record TransactionResponse(
        Long id,
        String transactionCode,
        TransactionType transactionType,
        TransactionStatus status,
        TransactionDirection direction,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        String description,
        String fromAccountNumber,
        String toAccountNumber,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}