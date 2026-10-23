package project.smartbanking.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OtpSendResponse(
        String message,
        long expiresInSeconds,
        // Chỉ có giá trị khi app.otp.expose-in-response=true (môi trường phát triển)
        String devOtp) {
}