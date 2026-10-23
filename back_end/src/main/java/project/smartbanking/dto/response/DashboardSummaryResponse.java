package project.smartbanking.dto.response;

import java.math.BigDecimal;

public record DashboardSummaryResponse(
        BigDecimal totalBalanceVnd,
        BigDecimal totalBalanceUsd,
        long accountCount,
        long transactionsThisMonth,
        long unreadNotifications) {
}