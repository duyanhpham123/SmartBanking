package project.smartbanking.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ChartResponse<T>(String currency, List<T> points) {

    // Biểu đồ của người dùng: tiền vào / tiền ra tính theo các tài khoản của chính họ
    public record DailyPoint(
            LocalDate date,
            long count,
            BigDecimal totalAmount,
            BigDecimal totalIn,
            BigDecimal totalOut) {
    }

    // Biểu đồ toàn hệ thống
    public record AdminDailyPoint(
            LocalDate date,
            long count,
            BigDecimal totalAmount) {
    }
}