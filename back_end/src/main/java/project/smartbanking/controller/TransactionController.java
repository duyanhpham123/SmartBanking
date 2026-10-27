package project.smartbanking.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.jspecify.annotations.NonNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.smartbanking.dto.request.DepositRequest;
import project.smartbanking.dto.request.TransferRequest;
import project.smartbanking.dto.request.WithdrawRequest;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.IdempotencyService;
import project.smartbanking.service.TransactionHistoryService;
import project.smartbanking.service.TransactionService;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final TransactionService transactionService;
    private final IdempotencyService idempotencyService;
    private final TransactionHistoryService historyService;

    public TransactionController(TransactionService transactionService,
                                 IdempotencyService idempotencyService,
                                 TransactionHistoryService historyService) {
        this.transactionService = transactionService;
        this.idempotencyService = idempotencyService;
        this.historyService = historyService;
    }

    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(@AuthenticationPrincipal  UserPrincipal principal,
                                                       @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.deposit(principal.getId(), request));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(@AuthenticationPrincipal  UserPrincipal principal,
                                                        @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transactionService.withdraw(principal.getId(), request));
    }

    // Header Idempotency-Key bắt buộc. Gửi lại cùng mã thì nhận lại kết quả cũ, không chuyển lần nữa.
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @AuthenticationPrincipal  UserPrincipal principal,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody TransferRequest request) {

        // OTP cố ý không đưa vào dấu vân tay: gửi lại có thể kèm OTP khác mà vẫn là cùng một yêu cầu
        String hash = idempotencyService.fingerprint(
                request.fromAccountId(),
                request.toAccountNumber().replaceAll("[\\s-]", ""),
                request.amount().stripTrailingZeros().toPlainString(),
                request.description() == null ? "" : request.description().trim());

        IdempotencyService.IdempotentResult<TransactionResponse> result = idempotencyService.execute(
                principal.getId(), idempotencyKey, hash,
                TransactionResponse.class, TransactionResponse::id,
                () -> transactionService.transfer(principal.getId(), request));

        return ResponseEntity.status(HttpStatus.CREATED)
                .header("Idempotent-Replayed", String.valueOf(result.replayed()))
                .body(result.response());
    }

    // Lịch sử giao dịch: from/to dạng yyyy-MM-dd, ví dụ ?from=2026-10-01&to=2026-10-07
    @GetMapping("/history")
    public ResponseEntity<PageResponse<TransactionResponse>> getHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(transactionService.getHistory(
                principal.getId(), type, status, from, to, keyword, page, size));
    }

    // Xuất lịch sử ra CSV (tối đa 5000 dòng mới nhất), dùng cùng bộ lọc với danh sách
    @GetMapping("/history/export")
    public ResponseEntity<byte[]> export(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword) {
        String csv = historyService.exportCsv(principal.getId(), type, status, from, to, keyword);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lich-su-giao-dich.csv\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponse> getTransaction(@AuthenticationPrincipal UserPrincipal principal,
                                                              @PathVariable Long id) {
        return ResponseEntity.ok(transactionService.getTransaction(principal.getId(), id));
    }
}