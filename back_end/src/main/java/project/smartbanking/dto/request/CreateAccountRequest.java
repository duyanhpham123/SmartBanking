package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import project.smartbanking.model.enums.AccountType;

public record CreateAccountRequest(
        @NotNull(message = "Vui lòng chọn loại tài khoản")
        AccountType accountType,

        @NotBlank(message = "Vui lòng chọn loại tiền tệ")
        @Pattern(regexp = "^(VND|USD)$", message = "Chỉ hỗ trợ VND hoặc USD")
        String currency,

        // Có thể bỏ trống, hệ thống sẽ đặt tên mặc định
        @Size(max = 150, message = "Tên tài khoản tối đa 150 ký tự")
        String accountName) {
}