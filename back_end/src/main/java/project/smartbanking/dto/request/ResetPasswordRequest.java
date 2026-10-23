package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Vui lòng nhập số điện thoại hoặc email")
        String identifier,

        @NotBlank(message = "Mã OTP không được để trống")
        @Pattern(regexp = "^\\d{6}$", message = "Mã OTP gồm 6 chữ số")
        String otp,

        @NotBlank(message = "Mật khẩu mới không được để trống")
        @Size(min = 8, max = 64, message = "Mật khẩu từ 8 đến 64 ký tự")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Mật khẩu phải có cả chữ và số")
        String newPassword) {
}