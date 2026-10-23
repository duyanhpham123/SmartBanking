package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordRequest(
        @NotBlank(message = "Vui lòng nhập số điện thoại hoặc email")
        String identifier) {
}