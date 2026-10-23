package project.smartbanking.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import project.smartbanking.model.enums.PaymentMethod;

public record DepositRequest(
        @NotNull(message = "Vui lòng chọn tài khoản nhận")
        Long accountId,

        @NotNull(message = "Vui lòng nhập số tiền")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Digits(integer = 15, fraction = 2, message = "Số tiền không hợp lệ")
        BigDecimal amount,

        @NotNull(message = "Vui lòng chọn phương thức nạp")
        PaymentMethod method) {
}