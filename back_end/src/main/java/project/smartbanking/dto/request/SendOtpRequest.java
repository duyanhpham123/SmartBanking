package project.smartbanking.dto.request;

import jakarta.validation.constraints.NotNull;
import project.smartbanking.model.enums.OtpPurpose;

public record SendOtpRequest(
        @NotNull(message = "Vui lòng chọn mục đích OTP")
        OtpPurpose purpose) {
}