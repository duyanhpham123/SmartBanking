package project.smartbanking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.smartbanking.dto.request.SendOtpRequest;
import project.smartbanking.dto.request.VerifyOtpRequest;
import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.OtpSendResponse;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.OtpService;

@RestController
@RequestMapping("/api/v1/otp")
public class OtpController {

    private final OtpService otpService;

    public OtpController(OtpService otpService) {
        this.otpService = otpService;
    }

    @PostMapping("/send")
    public ResponseEntity<OtpSendResponse> send(@AuthenticationPrincipal UserPrincipal principal,
                                                @Valid @RequestBody SendOtpRequest request) {
        return ResponseEntity.ok(otpService.send(principal.getId(), request.purpose()));
    }

    // Chỉ kiểm tra mã còn hợp lệ hay không, KHÔNG tiêu hủy mã.
    // Mã chỉ bị tiêu hủy khi dùng thật (ví dụ khi gọi API chuyển tiền).
    @PostMapping("/verify")
    public ResponseEntity<MessageResponse> verify(@AuthenticationPrincipal UserPrincipal principal,
                                                  @Valid @RequestBody VerifyOtpRequest request) {
        otpService.verify(principal.getId(), request.purpose(), request.code());
        return ResponseEntity.ok(new MessageResponse("Mã OTP hợp lệ"));
    }
}