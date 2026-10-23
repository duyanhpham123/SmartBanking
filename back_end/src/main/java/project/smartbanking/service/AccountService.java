package project.smartbanking.service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.request.CreateAccountRequest;
import project.smartbanking.dto.request.RenameAccountRequest;
import project.smartbanking.dto.response.AccountLookupResponse;
import project.smartbanking.dto.response.AccountResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.AccountMapper;
import project.smartbanking.model.Account;
import project.smartbanking.model.User;
import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.AccountType;
import project.smartbanking.repository.AccountRepository;
import project.smartbanking.repository.UserRepository;
import project.smartbanking.util.AuditContext;

@Service
public class AccountService {

    private static final int MAX_ACCOUNTS_PER_USER = 10;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;
    private final AccountMapper accountMapper;

    public AccountService(AccountRepository accountRepository,
                          UserRepository userRepository,
                          AccountMapper accountMapper) {
        this.accountRepository = accountRepository;
        this.userRepository = userRepository;
        this.accountMapper = accountMapper;
    }

    // ===== Danh sách tài khoản của tôi =====
    @Transactional(readOnly = true)
    public List<AccountResponse> getMyAccounts(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED)
                .sorted(Comparator.comparing(Account::getId))
                .map(accountMapper::toResponse)
                .toList();
    }

    // ===== Chi tiết một tài khoản (chỉ của chính mình) =====
    @Transactional(readOnly = true)
    public AccountResponse getMyAccount(Long userId, Long accountId) {
        return accountMapper.toResponse(findOwnedAccount(userId, accountId));
    }

    // ===== Tra cứu người nhận theo số tài khoản =====
    @Transactional(readOnly = true)
    public AccountLookupResponse lookup(String accountNumber) {
        String number = accountNumber == null ? "" : accountNumber.replaceAll("[\\s-]", "");
        if (!number.matches("\\d{8,30}")) {
            throw new BadRequestException("Số tài khoản không hợp lệ");
        }

        Account account = accountRepository.findByAccountNumber(number)
                .filter(a -> a.getStatus() == AccountStatus.ACTIVE)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản nhận"));

        return accountMapper.toLookupResponse(account);
    }

    // ===== Mở tài khoản mới =====
    @Transactional
    public AccountResponse createAccount(Long userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));

        if (accountRepository.countByUserId(userId) >= MAX_ACCOUNTS_PER_USER) {
            throw new BadRequestException("Bạn chỉ được mở tối đa " + MAX_ACCOUNTS_PER_USER + " tài khoản");
        }

        String name = (request.accountName() == null || request.accountName().isBlank())
                ? defaultName(request.accountType(), request.currency())
                : request.accountName().trim();

        Account account = buildAccount(user, request.accountType(), request.currency(), name);
        Account saved = accountRepository.save(account);
        AuditContext.setEntity("ACCOUNT", saved.getId());
        return accountMapper.toResponse(saved);
    }

    // ===== Tài khoản mặc định cho người mới đăng ký (AuthService gọi) =====
    @Transactional
    public Account createDefaultAccount(User user) {
        Account account = buildAccount(user, AccountType.PAYMENT, "VND",
                defaultName(AccountType.PAYMENT, "VND"));
        return accountRepository.save(account);
    }

    // ===== Đổi tên tài khoản =====
    @Transactional
    public AccountResponse rename(Long userId, Long accountId, RenameAccountRequest request) {
        Account account = findOwnedAccount(userId, accountId);
        account.setAccountName(request.accountName().trim());
        return accountMapper.toResponse(accountRepository.save(account));
    }

    // ===== Hàm phụ =====

    // Tài khoản không thuộc người dùng cũng báo 404, để không lộ việc nó có tồn tại
    private Account findOwnedAccount(Long userId, Long accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ResourceNotFoundException("Không tìm thấy tài khoản");
        }
        return account;
    }

    private Account buildAccount(User user, AccountType type, String currency, String name) {
        Account account = new Account();
        account.setUser(user);
        account.setAccountNumber(generateAccountNumber());
        account.setAccountName(name);
        account.setAccountType(type);
        account.setCurrency(currency);
        account.setBalance(BigDecimal.ZERO);
        account.setAvailableBalance(BigDecimal.ZERO);
        account.setStatus(AccountStatus.ACTIVE);
        return account;
    }

    private String defaultName(AccountType type, String currency) {
        String base = type == AccountType.SAVINGS ? "Tài khoản tiết kiệm"
                : "USD".equals(currency) ? "Tài khoản ngoại tệ" : "Tài khoản thanh toán";
        return "USD".equals(currency) && type == AccountType.SAVINGS ? base + " (USD)"
                : "USD".equals(currency) ? base + " (USD)" : base;
    }

    // 16 chữ số, chữ số đầu khác 0, đảm bảo chưa tồn tại
    private String generateAccountNumber() {
        for (int i = 0; i < 20; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append(1 + RANDOM.nextInt(9));
            for (int j = 1; j < 16; j++) {
                sb.append(RANDOM.nextInt(10));
            }
            String number = sb.toString();
            if (!accountRepository.existsByAccountNumber(number)) {
                return number;
            }
        }
        throw new IllegalStateException("Không thể sinh số tài khoản, vui lòng thử lại");
    }
}