package project.smartbanking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionLimitResponse(
        String currency,
        BigDecimal dailyOutLimit,
        BigDecimal defaultDailyOutLimit,
        BigDecimal maxDailyOutLimit,
        LocalDateTime updatedAt,
        String updatedByName) {
}
