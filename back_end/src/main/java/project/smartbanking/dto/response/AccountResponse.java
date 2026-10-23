package project.smartbanking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.AccountType;

public record AccountResponse(
        Long id,
        String accountNumber,
        String accountName,
        AccountType accountType,
        String currency,
        BigDecimal balance,
        BigDecimal availableBalance,
        AccountStatus status,
        LocalDateTime createdAt) {
}