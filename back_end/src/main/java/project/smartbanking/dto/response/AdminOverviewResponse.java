package project.smartbanking.dto.response;

import java.math.BigDecimal;

public record AdminOverviewResponse(
        long totalUsers,
        long totalAccounts,
        long totalTransactions,
        BigDecimal totalAmountVnd,
        BigDecimal totalAmountUsd) {
}