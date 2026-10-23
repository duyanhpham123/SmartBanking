package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import project.smartbanking.model.enums.OtpPurpose;

public record VerifyOtpRequest(
        @NotNull(message = "Vui lòng chọn mục đích OTP")
        OtpPurpose purpose,

        @NotBlank(message = "Vui lòng nhập mã OTP")
        @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số")
        String code) {
}