package project.smartbanking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;

public record AdminTransactionResponse(
        Long id,
        String transactionCode,
        TransactionType transactionType,
        TransactionStatus status,
        BigDecimal amount,
        BigDecimal fee,
        String currency,
        String description,
        String failureReason,
        String fromAccountNumber,
        String fromUsername,
        String toAccountNumber,
        String toUsername,
        LocalDateTime createdAt,
        LocalDateTime completedAt) {
}