package project.smartbanking.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import project.smartbanking.model.Account;
import project.smartbanking.model.enums.AccountStatus;
import project.smartbanking.model.enums.AccountType;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    // ===== Người dùng: danh sách / chi tiết tài khoản của tôi =====
    List<Account> findByUserId(Long userId);

    List<Account> findByUserIdAndStatus(Long userId, AccountStatus status);

    // Dùng khi xem chi tiết: đảm bảo tài khoản thuộc đúng người đang đăng nhập
    Optional<Account> findByIdAndUserId(Long id, Long userId);

    // ===== Tra cứu theo số tài khoản (màn Chuyển tiền: hiện tên người nhận) =====
    Optional<Account> findByAccountNumber(String accountNumber);

    // Dùng khi sinh số tài khoản mới để tránh trùng
    boolean existsByAccountNumber(String accountNumber);

    // ===== Khóa dòng khi nạp / rút / chuyển tiền =====
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.id = :id")
    Optional<Account> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from Account a where a.accountNumber = :accountNumber")
    Optional<Account> findByAccountNumberForUpdate(@Param("accountNumber") String accountNumber);

    // ===== Dashboard: tổng số dư theo loại tiền =====
    @Query("select coalesce(sum(a.balance), 0) from Account a "
            + "where a.user.id = :userId and a.currency = :currency and a.status = :status")
    BigDecimal sumBalanceByUserIdAndCurrency(@Param("userId") Long userId,
                                             @Param("currency") String currency,
                                             @Param("status") AccountStatus status);

    long countByUserId(Long userId);

    // ===== Admin: danh sách, thống kê =====
    Page<Account> findByStatus(AccountStatus status, Pageable pageable);

    long countByStatus(AccountStatus status);

    // status, type, currency, keyword có thể null = không lọc.
    // keyword đã xử lý sẵn dạng "%abc%" (chữ thường).
    @EntityGraph(attributePaths = { "user" })
    @Query("""
            select a from Account a
            where (:status is null or a.status = :status)
              and (:type is null or a.accountType = :type)
              and (:currency is null or a.currency = :currency)
              and (:keyword is null
                   or a.accountNumber like :keyword
                   or lower(a.accountName) like :keyword
                   or lower(a.user.username) like :keyword
                   or lower(a.user.fullName) like :keyword)
            """)
    Page<Account> search(@Param("status") AccountStatus status,
                         @Param("type") AccountType type,
                         @Param("currency") String currency,
                         @Param("keyword") String keyword,
                         Pageable pageable);
}