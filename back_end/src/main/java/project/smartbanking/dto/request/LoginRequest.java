package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        // Email hoặc số điện thoại (đúng như ô nhập trên màn đăng nhập)
        @NotBlank(message = "Vui lòng nhập số điện thoại hoặc email")
        String identifier,

        @NotBlank(message = "Mật khẩu không được để trống")
        String password,

        // Ô "Ghi nhớ đăng nhập"
        boolean rememberMe) {
}