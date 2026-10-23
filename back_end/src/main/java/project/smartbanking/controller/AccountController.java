package project.smartbanking.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import project.smartbanking.dto.request.CreateAccountRequest;
import project.smartbanking.dto.request.RenameAccountRequest;
import project.smartbanking.dto.response.AccountLookupResponse;
import project.smartbanking.dto.response.AccountResponse;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.AccountService;
import project.smartbanking.service.TransactionHistoryService;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final AccountService accountService;
    private final TransactionHistoryService historyService;

    public AccountController(AccountService accountService, TransactionHistoryService historyService) {
        this.accountService = accountService;
        this.historyService = historyService;
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getMyAccounts(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(accountService.getMyAccounts(principal.getId()));
    }

    @GetMapping("/lookup")
    public ResponseEntity<AccountLookupResponse> lookup(@RequestParam String accountNumber) {
        return ResponseEntity.ok(accountService.lookup(accountNumber));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AccountResponse> getMyAccount(@AuthenticationPrincipal UserPrincipal principal,
                                                        @PathVariable Long id) {
        return ResponseEntity.ok(accountService.getMyAccount(principal.getId(), id));
    }

    // Lịch sử giao dịch của riêng một tài khoản: from/to dạng yyyy-MM-dd
    @GetMapping("/{id}/transactions")
    public ResponseEntity<PageResponse<TransactionResponse>> getAccountTransactions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) TransactionStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(historyService.getAccountHistory(
                principal.getId(), id, type, status, from, to, keyword, page, size));
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@AuthenticationPrincipal UserPrincipal principal,
                                                         @Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(accountService.createAccount(principal.getId(), request));
    }

    @PutMapping("/{id}/name")
    public ResponseEntity<AccountResponse> rename(@AuthenticationPrincipal UserPrincipal principal,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody RenameAccountRequest request) {
        return ResponseEntity.ok(accountService.rename(principal.getId(), id, request));
    }
}