package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RejectKycRequest(
        @NotBlank(message = "Vui lòng nhập lý do từ chối")
        @Size(max = 500, message = "Lý do tối đa 500 ký tự")
        String note) {
}
