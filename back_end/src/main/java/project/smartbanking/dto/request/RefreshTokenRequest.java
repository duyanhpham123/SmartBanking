package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;

// Dùng chung cho POST /auth/refresh và POST /auth/logout
public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token không được để trống")
        String refreshToken) {
}