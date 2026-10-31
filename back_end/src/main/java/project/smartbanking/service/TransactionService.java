package project.smartbanking.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.request.DepositRequest;
import project.smartbanking.dto.request.PaymentRequest;
import project.smartbanking.dto.request.TransferRequest;
import project.smartbanking.dto.request.WithdrawRequest;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.TransactionResponse;
import project.smartbanking.event.TransactionCompletedEvent;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.InsufficientBalanceException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.TransactionMapper;
import project.smartbanking.model.Account;
import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.OtpPurpose;
import project.smartbanking.model.enums.PaymentMethod;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.repository.AccountRepository;
import project.smartbanking.repository.TransactionRepository;
import project.smartbanking.util.AuditContext;

@Service
public class TransactionService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    // Hạn mức mỗi lần nạp
    private static final BigDecimal MIN_DEPOSIT_VND = new BigDecimal("10000");
    private static final BigDecimal MAX_DEPOSIT_VND = new BigDecimal("500000000");
    private static final BigDecimal MIN_DEPOSIT_USD = BigDecimal.ONE;
    private static final BigDecimal MAX_DEPOSIT_USD = new BigDecimal("20000");

    // Hạn mức mỗi lần rút
    private static final BigDecimal MIN_WITHDRAW_VND = new BigDecimal("10000");
    private static final BigDecimal MAX_WITHDRAW_VND = new BigDecimal("50000000");
    private static final BigDecimal MIN_WITHDRAW_USD = BigDecimal.ONE;
    private static final BigDecimal MAX_WITHDRAW_USD = new BigDecimal("10000");

    // Hạn mức mỗi lần thanh toán hóa đơn (chỉ VND)
    private static final BigDecimal MIN_PAYMENT_VND = new BigDecimal("1000");
    private static final BigDecimal MAX_PAYMENT_VND = new BigDecimal("50000000");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final OtpService otpService;
    // Hạn mức tổng tiền ra (rút + chuyển + thanh toán) mỗi ngày: admin chỉnh trên trang quản trị
    private final TransactionLimitService limitService;

    public TransactionService(AccountRepository accountRepository,
            TransactionRepository transactionRepository,
            TransactionMapper transactionMapper,
            ApplicationEventPublisher eventPublisher,
            OtpService otpService,
            TransactionLimitService limitService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transactionMapper = transactionMapper;
        this.eventPublisher = eventPublisher;
        this.otpService = otpService;
        this.limitService = limitService;
    }

    // ===================== NẠP TIỀN =====================
    @Transactional
    public TransactionResponse deposit(Long userId, DepositRequest request) {
        Account account = lockOwnedActiveAccount(userId, request.accountId());

        BigDecimal amount = normalizeAmount(request.amount(), account.getCurrency(),
                MIN_DEPOSIT_VND, MAX_DEPOSIT_VND, MIN_DEPOSIT_USD, MAX_DEPOSIT_USD, "nạp");

        account.setBalance(account.getBalance().add(amount));
        account.setAvailableBalance(account.getAvailableBalance().add(amount));
        accountRepository.save(account);

        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionCode(generateTransactionCode());
        transaction.setToAccount(account);
        transaction.setAmount(amount);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency(account.getCurrency());
        transaction.setTransactionType(TransactionType.DEPOSIT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Nạp tiền qua " + request.method().getLabel());
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        publishCompleted(transaction);
        return transactionMapper.toResponse(transaction, Set.of(account.getId()));
    }

    // ===================== RÚT TIỀN =====================
    @Transactional
    public TransactionResponse withdraw(Long userId, WithdrawRequest request) {
        if (request.method() == PaymentMethod.ATM_CARD) {
            throw new BadRequestException("Rút tiền chỉ hỗ trợ chuyển khoản ngân hàng hoặc ví điện tử");
        }

        Account account = lockOwnedActiveAccount(userId, request.accountId());

        BigDecimal amount = normalizeAmount(request.amount(), account.getCurrency(),
                MIN_WITHDRAW_VND, MAX_WITHDRAW_VND, MIN_WITHDRAW_USD, MAX_WITHDRAW_USD, "rút");

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        checkDailyLimit(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        accountRepository.save(account);

        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionCode(generateTransactionCode());
        transaction.setFromAccount(account);
        transaction.setAmount(amount);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency(account.getCurrency());
        transaction.setTransactionType(TransactionType.WITHDRAW);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Rút tiền qua " + request.method().getLabel());
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        publishCompleted(transaction);

        return transactionMapper.toResponse(transaction, Set.of(account.getId()));
    }

    // ===================== CHUYỂN TIỀN =====================
    @Transactional
    public TransactionResponse transfer(Long userId, TransferRequest request) {
        otpService.verifyAndConsume(userId, OtpPurpose.TRANSFER, request.otp());

        String normalizedTargetNumber = normalizeAccountNumber(request.toAccountNumber());

        Account fromAccount = accountRepository.findByIdAndUserId(request.fromAccountId(), userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        Account toAccount = accountRepository.findByAccountNumber(normalizedTargetNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản nhận"));

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new BadRequestException("Không thể chuyển tiền cho chính tài khoản của bạn");
        }

        Account first = fromAccount.getId() <= toAccount.getId() ? fromAccount : toAccount;
        Account second = fromAccount.getId() <= toAccount.getId() ? toAccount : fromAccount;

        Account lockedFirst = accountRepository.findByIdForUpdate(first.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        Account lockedSecond = accountRepository.findByIdForUpdate(second.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        Account lockedFromAccount = lockedFirst.getId().equals(fromAccount.getId()) ? lockedFirst : lockedSecond;
        Account lockedToAccount = lockedSecond.getId().equals(toAccount.getId()) ? lockedSecond : lockedFirst;

        if (lockedFromAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản nguồn đang bị khóa hoặc không hoạt động");
        }

        if (lockedToAccount.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản nhận đang không hoạt động");
        }

        if (!lockedFromAccount.getCurrency().equals(lockedToAccount.getCurrency())) {
            throw new BadRequestException("Chỉ chuyển khoản được giữa hai tài khoản cùng loại tiền tệ");
        }

        BigDecimal amount = normalizeAmount(request.amount(), lockedFromAccount.getCurrency(),
                MIN_WITHDRAW_VND, MAX_WITHDRAW_VND, MIN_WITHDRAW_USD, MAX_WITHDRAW_USD, "chuyển khoản");

        if (lockedFromAccount.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        checkDailyLimit(lockedFromAccount, amount);

        lockedFromAccount.setBalance(lockedFromAccount.getBalance().subtract(amount));
        lockedFromAccount.setAvailableBalance(lockedFromAccount.getAvailableBalance().subtract(amount));

        lockedToAccount.setBalance(lockedToAccount.getBalance().add(amount));
        lockedToAccount.setAvailableBalance(lockedToAccount.getAvailableBalance().add(amount));

        accountRepository.save(lockedFromAccount);
        accountRepository.save(lockedToAccount);

        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionCode(generateTransactionCode());
        transaction.setFromAccount(lockedFromAccount);
        transaction.setToAccount(lockedToAccount);
        transaction.setAmount(amount);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency(lockedFromAccount.getCurrency());
        transaction.setTransactionType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription(request.description() == null || request.description().isBlank()
                ? "Chuyển tiền" : request.description());
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        publishCompleted(transaction);

        return transactionMapper.toResponse(transaction, Set.of(lockedFromAccount.getId(), lockedToAccount.getId()));
    }

    // ===================== HÀM PHỤ =====================

    private Account lockOwnedActiveAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Tài khoản đang bị khóa hoặc không hoạt động");
        }
        return account;
    }

    private BigDecimal normalizeAmount(BigDecimal input, String currency,
                                       BigDecimal minVnd, BigDecimal maxVnd,
                                       BigDecimal minUsd, BigDecimal maxUsd, String action) {
        BigDecimal amount = input.stripTrailingZeros();

        if ("VND".equals(currency)) {
            if (amount.scale() > 0) {
                throw new BadRequestException("Số tiền VND phải là số nguyên");
            }
            checkRange(input, minVnd, maxVnd, "VND", action);
        } else if ("USD".equals(currency)) {
            checkRange(input, minUsd, maxUsd, "USD", action);
        } else {
            throw new BadRequestException("Loại tiền tệ " + currency + " chưa được hỗ trợ");
        }

        return amount.setScale(2, RoundingMode.UNNECESSARY);
    }

    // Gọi khi tài khoản nguồn đã bị khóa dòng (FOR UPDATE), nên các giao dịch song song
    // trên cùng tài khoản phải xếp hàng và tổng trong ngày luôn chính xác.
    private void checkDailyLimit(Account account, BigDecimal amount) {
        BigDecimal limit = limitService.getDailyOutLimit(account.getCurrency());
        BigDecimal usedToday = transactionRepository.sumOutgoingSince(
                account.getId(), LocalDate.now().atStartOfDay());
        if (usedToday == null) {
            usedToday = BigDecimal.ZERO;
        }
        if (usedToday.add(amount).compareTo(limit) > 0) {
            BigDecimal remaining = limit.subtract(usedToday).max(BigDecimal.ZERO);
            throw new BadRequestException("Vượt hạn mức giao dịch trong ngày. Hạn mức còn lại: "
                    + remaining.stripTrailingZeros().toPlainString() + " " + account.getCurrency());
        }
    }

    private void checkRange(BigDecimal amount, BigDecimal min, BigDecimal max, String currency, String action) {
        if (amount.compareTo(min) < 0) {
            throw new BadRequestException("Số tiền " + action + " tối thiểu là " + min.toPlainString() + " " + currency);
        }
        if (amount.compareTo(max) > 0) {
            throw new BadRequestException("Số tiền " + action + " tối đa mỗi lần là " + max.toPlainString() + " " + currency);
        }
    }

    private String normalizeAccountNumber(String accountNumber) {
        if (accountNumber == null) {
            throw new BadRequestException("Vui lòng nhập số tài khoản nhận");
        }

        String sanitized = accountNumber.replaceAll("\\s+", "");
        if (!sanitized.matches("\\d{16}")) {
            throw new BadRequestException("Số tài khoản không hợp lệ");
        }
        return sanitized;
    }

    private String generateTransactionCode() {
        for (int i = 0; i < 10; i++) {
            String code = "TXN" + LocalDateTime.now().format(CODE_TIME)
                    + String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!transactionRepository.existsByTransactionCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("Không thể sinh mã giao dịch, vui lòng thử lại");
    }

    // ===================== THANH TOÁN HÓA ĐƠN =====================
    @Transactional
    public TransactionResponse pay(Long userId, PaymentRequest request) {
        otpService.verifyAndConsume(userId, OtpPurpose.TRANSFER, request.otp());

        Account account = lockOwnedActiveAccount(userId, request.accountId());

        if (!"VND".equals(account.getCurrency())) {
            throw new BadRequestException("Chỉ thanh toán hóa đơn bằng tài khoản VND");
        }

        BigDecimal amount = normalizeAmount(request.amount(), account.getCurrency(),
                MIN_PAYMENT_VND, MAX_PAYMENT_VND, BigDecimal.ONE, BigDecimal.ONE, "thanh toán");

        if (account.getAvailableBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException();
        }
        checkDailyLimit(account, amount);

        account.setBalance(account.getBalance().subtract(amount));
        account.setAvailableBalance(account.getAvailableBalance().subtract(amount));
        accountRepository.save(account);

        BankTransaction transaction = new BankTransaction();
        transaction.setTransactionCode(generateTransactionCode());
        transaction.setFromAccount(account);
        transaction.setAmount(amount);
        transaction.setFee(BigDecimal.ZERO);
        transaction.setCurrency(account.getCurrency());
        transaction.setTransactionType(TransactionType.PAYMENT);
        transaction.setStatus(TransactionStatus.SUCCESS);
        transaction.setDescription("Thanh toán hóa đơn " + request.provider().getLabel()
                + " - Mã KH " + request.customerCode().trim().toUpperCase());
        transaction.setCompletedAt(LocalDateTime.now());
        transactionRepository.save(transaction);
        publishCompleted(transaction);

        return transactionMapper.toResponse(transaction, Set.of(account.getId()));
    }

    // ===================== LỊCH SỬ GIAO DỊCH =====================
    @Transactional(readOnly = true)
    public PageResponse<TransactionResponse> getHistory(Long userId, TransactionType type,
                                                        TransactionStatus status,
                                                        LocalDate from, LocalDate to, String keyword,
                                                        int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 10 : Math.min(size, 100);

        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        List<Long> accountIds = myAccountIds(userId);
        if (accountIds.isEmpty()) {
            return new PageResponse<>(List.of(), safePage, safeSize, 0, 0);
        }

        LocalDateTime fromDate = from == null
                ? LocalDateTime.of(1970, 1, 1, 0, 0)
                : from.atStartOfDay();
        LocalDateTime toDate = to == null
                ? LocalDate.now().plusDays(1).atStartOfDay()
                : to.plusDays(1).atStartOfDay();
        String kw = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim().toLowerCase() + "%";

        Pageable pageable = PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        Page<BankTransaction> result = transactionRepository.searchHistory(
                accountIds, type, status, fromDate, toDate, kw, pageable);

        Set<Long> mine = Set.copyOf(accountIds);
        return PageResponse.from(result.map(t -> transactionMapper.toResponse(t, mine)));
    }

    // ===================== CHI TIẾT GIAO DỊCH =====================
    @Transactional(readOnly = true)
    public TransactionResponse getTransaction(Long userId, Long transactionId) {
        List<Long> accountIds = myAccountIds(userId);
        if (accountIds.isEmpty()) {
            throw new ResourceNotFoundException("Không tìm thấy giao dịch");
        }

        BankTransaction transaction = transactionRepository.findByIdAndAccountIds(transactionId, accountIds)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));

        return transactionMapper.toResponse(transaction, Set.copyOf(accountIds));
    }

    private List<Long> myAccountIds(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(Account::getId)
                .toList();
    }

    private void publishCompleted(BankTransaction t) {
        AuditContext.setEntity("TRANSACTION", t.getId());
        Account from = t.getFromAccount();
        Account to = t.getToAccount();
        eventPublisher.publishEvent(new TransactionCompletedEvent(
                t.getId(),
                t.getTransactionType(),
                from == null ? null : from.getUser().getId(),
                to == null ? null : to.getUser().getId(),
                from == null ? null : from.getAccountNumber(),
                to == null ? null : to.getAccountNumber(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription()));
    }
}