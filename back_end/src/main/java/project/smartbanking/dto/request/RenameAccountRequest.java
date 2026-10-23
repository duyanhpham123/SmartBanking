package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameAccountRequest(
        @NotBlank(message = "Tên tài khoản không được để trống")
        @Size(max = 150, message = "Tên tài khoản tối đa 150 ký tự")
        String accountName) {
}