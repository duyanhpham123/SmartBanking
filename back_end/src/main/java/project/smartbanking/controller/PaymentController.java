package project.smartbanking.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.smartbanking.dto.request.PaymentRequest;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.IdempotencyService;
import project.smartbanking.service.TransactionService;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;

    public PaymentController(TransactionService transactionService,
                             IdempotencyService idempotencyService) {
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
    }

    // Header Idempotency-Key bắt buộc. Gửi lại cùng mã thì nhận lại kết quả cũ, không thanh toán lần nữa.
    @PostMapping
    public ResponseEntity<TransactionResponse> pay(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PaymentRequest request) {

        // Có tiền tố "PAYMENT" để không trùng dấu vân tay với chuyển tiền; OTP không đưa vào
        String hash = idempotencyService.fingerprint(
                "PAYMENT",
                request.accountId(),
                request.provider(),
                request.customerCode().trim().toUpperCase(),
                request.amount().stripTrailingZeros().toPlainString());

        IdempotencyService.IdempotentResult<TransactionResponse> result = idempotencyService.execute(
                principal.getId(), idempotencyKey, hash,
                TransactionResponse.class, TransactionResponse::id,
                () -> transactionService.pay(principal.getId(), request));

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(result.response());
    }
}