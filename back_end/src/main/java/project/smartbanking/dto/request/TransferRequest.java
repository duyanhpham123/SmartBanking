package project.smartbanking.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TransferRequest(
        @NotNull(message = "Vui lòng chọn tài khoản nguồn")
        Long fromAccountId,

        @NotBlank(message = "Vui lòng nhập số tài khoản nhận")
        @Size(max = 40, message = "Số tài khoản không hợp lệ")
        String toAccountNumber,

        @NotNull(message = "Vui lòng nhập số tiền")
        @DecimalMin(value = "0.01", message = "Số tiền phải lớn hơn 0")
        @Digits(integer = 15, fraction = 2, message = "Số tiền không hợp lệ")
        BigDecimal amount,

        @Size(max = 200, message = "Nội dung tối đa 200 ký tự")
        String description,

        @NotBlank(message = "Vui lòng nhập mã OTP")
        @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số")
        String otp) {
}