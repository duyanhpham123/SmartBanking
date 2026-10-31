package project.smartbanking.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record UpdateTransactionLimitRequest(
        @NotNull(message = "Vui lòng nhập hạn mức")
        @Positive(message = "Hạn mức phải lớn hơn 0")
        @Digits(integer = 15, fraction = 2, message = "Hạn mức không hợp lệ")
        BigDecimal dailyOutLimit) {
}
