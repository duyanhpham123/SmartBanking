package project.smartbanking.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import project.smartbanking.model.enums.BillProvider;

public record PaymentRequest(
        @NotNull(message = "Vui lòng chọn tài khoản thanh toán")
        Long accountId,

        @NotNull(message = "Vui lòng chọn nhà cung cấp")
        BillProvider provider,

        @NotBlank(message = "Vui lòng nhập mã khách hàng")
        @Pattern(regexp = "^[A-Za-z0-9]{6,20}$", message = "Mã khách hàng gồm 6-20 chữ và số")
        String customerCode,

        @NotNull(message = "Vui lòng nhập số tiền")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Digits(integer = 15, fraction = 2, message = "Số tiền không hợp lệ")
        BigDecimal amount,

        @NotBlank(message = "Vui lòng nhập mã OTP")
        @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số")
        String otp) {
}