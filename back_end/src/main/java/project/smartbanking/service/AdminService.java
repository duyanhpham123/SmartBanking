package project.smartbanking.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.response.AccountResponse;
import project.smartbanking.dto.response.AdminAccountResponse;
import project.smartbanking.dto.response.AdminTransactionResponse;
import project.smartbanking.dto.response.AdminUserDetailResponse;
import project.smartbanking.dto.response.PageResponse;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.AccountMapper;
import project.smartbanking.mapper.AdminMapper;
import project.smartbanking.mapper.UserMapper;
import project.smartbanking.model.Account;
import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.User;
import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.AccountType;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;
import project.smartbanking.model.enums.UserStatus;
import project.smartbanking.repository.AccountRepository;
import project.smartbanking.repository.RefreshTokenRepository;
import project.smartbanking.repository.TransactionRepository;
import project.smartbanking.repository.UserRepository;

@Service
public class AdminService {

    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserMapper userMapper;
    private final AccountMapper accountMapper;
    private final AdminMapper adminMapper;

    public AdminService(UserRepository userRepository,
                        AccountRepository accountRepository,
                        TransactionRepository transactionRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        UserMapper userMapper,
                        AccountMapper accountMapper,
                        AdminMapper adminMapper) {
        this.userRepository = userRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userMapper = userMapper;
        this.accountMapper = accountMapper;
        this.adminMapper = adminMapper;
    }

    // ===================== NGƯỜI DÙNG =====================

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> getUsers(UserStatus status, String keyword, int page, int size) {
        Page<User> result = userRepository.search(status, like(keyword), pageable(page, size));
        return PageResponse.from(result.map(userMapper::toResponse));
    }

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(Long userId) {
        User user = findUser(userId);
        List<AccountResponse> accounts = accountRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Account::getId))
                .map(accountMapper::toResponse)
                .toList();
        return new AdminUserDetailResponse(userMapper.toResponse(user), accounts);
    }

    @Transactional
    public UserResponse lockUser(Long adminId, Long userId) {
        User user = findUser(userId);

        if (user.getId().equals(adminId)) {
            throw new BadRequestException("Bạn không thể tự khóa tài khoản của mình");
        }
        if (isAdmin(user)) {
            throw new BadRequestException("Không thể khóa tài khoản quản trị viên");
        }
        if (user.getStatus() == UserStatus.DELETED) {
            throw new BadRequestException("Người dùng đã bị xóa");
        }

        if (user.getStatus() != UserStatus.LOCKED) {
            user.setStatus(UserStatus.LOCKED);
            userRepository.saveAndFlush(user);
            // Lấy kết quả trước vì lệnh bên dưới làm sạch bộ nhớ đệm của Hibernate
            UserResponse response = userMapper.toResponse(user);
            // Đăng xuất mọi thiết bị; access token đang dùng bị chặn ngay vì trạng thái được kiểm tra mỗi request
            refreshTokenRepository.revokeAllByUserId(userId);
            return response;
        }
        return userMapper.toResponse(user);
    }

    @Transactional
    public UserResponse unlockUser(Long userId) {
        User user = findUser(userId);

        if (user.getStatus() == UserStatus.ACTIVE) {
            return userMapper.toResponse(user);
        }
        if (user.getStatus() != UserStatus.LOCKED) {
            throw new BadRequestException("Chỉ có thể mở khóa người dùng đang bị khóa");
        }

        user.setStatus(UserStatus.ACTIVE);
        return userMapper.toResponse(userRepository.save(user));
    }

    // ===================== TÀI KHOẢN NGÂN HÀNG =====================

    @Transactional(readOnly = true)
    public PageResponse<AdminAccountResponse> getAccounts(AccountStatus status, AccountType type,
                                                          String currency, String keyword,
                                                          int page, int size) {
        String cur = (currency == null || currency.isBlank()) ? null : currency.trim().toUpperCase();
        Page<Account> result = accountRepository.search(status, type, cur, like(keyword), pageable(page, size));
        return PageResponse.from(result.map(adminMapper::toAdminAccount));
    }

    @Transactional(readOnly = true)
    public AdminAccountResponse getAccount(Long accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));
        return adminMapper.toAdminAccount(account);
    }

    @Transactional
    public AdminAccountResponse lockAccount(Long accountId) {
        // Khóa dòng: nếu đang có giao dịch trên tài khoản này thì chờ giao dịch xong rồi mới đổi trạng thái
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new BadRequestException("Tài khoản đã đóng");
        }
        if (account.getStatus() != AccountStatus.LOCKED) {
            account.setStatus(AccountStatus.LOCKED);
            accountRepository.save(account);
        }
        return adminMapper.toAdminAccount(account);
    }

    @Transactional
    public AdminAccountResponse unlockAccount(Long accountId) {
        Account account = accountRepository.findByIdForUpdate(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản"));

        if (account.getStatus() == AccountStatus.ACTIVE) {
            return adminMapper.toAdminAccount(account);
        }
        if (account.getStatus() != AccountStatus.LOCKED) {
            throw new BadRequestException("Chỉ có thể mở khóa tài khoản đang bị khóa");
        }

        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);
        return adminMapper.toAdminAccount(account);
    }

    // ===================== GIAO DỊCH TOÀN HỆ THỐNG =====================

    @Transactional(readOnly = true)
    public PageResponse<AdminTransactionResponse> getTransactions(TransactionType type, TransactionStatus status,
                                                                  LocalDate from, LocalDate to, String keyword,
                                                                  int page, int size) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("Ngày bắt đầu phải trước hoặc bằng ngày kết thúc");
        }

        LocalDateTime fromDate = from == null
                ? LocalDateTime.of(1970, 1, 1, 0, 0)
                : from.atStartOfDay();
        // Điều kiện trong query là "< toDate", nên lấy đầu ngày hôm sau để gồm cả ngày cuối
        LocalDateTime toDate = to == null
                ? LocalDate.now().plusDays(1).atStartOfDay()
                : to.plusDays(1).atStartOfDay();

        Page<BankTransaction> result = transactionRepository.searchAll(
                type, status, fromDate, toDate, like(keyword), pageable(page, size));
        return PageResponse.from(result.map(adminMapper::toAdminTransaction));
    }

    @Transactional(readOnly = true)
    public AdminTransactionResponse getTransaction(Long transactionId) {
        BankTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy giao dịch"));
        return adminMapper.toAdminTransaction(transaction);
    }

    // ===================== HÀM PHỤ =====================

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(r -> "ROLE_ADMIN".equals(r.getName()));
    }

    // Mới nhất lên đầu; trang đánh số từ 0, mặc định 20 dòng, tối đa 100
    private Pageable pageable(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = size < 1 ? 20 : Math.min(size, 100);
        return PageRequest.of(safePage, safeSize,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    // Từ khóa -> "%abc%" chữ thường, hoặc null nếu để trống
    private String like(String keyword) {
        return (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim().toLowerCase() + "%";
    }
}