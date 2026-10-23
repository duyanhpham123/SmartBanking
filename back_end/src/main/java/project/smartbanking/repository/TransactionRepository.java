package project.smartbanking.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.BankTransaction;
import project.smartbanking.model.enums.TransactionStatus;
import project.smartbanking.model.enums.TransactionType;

@Repository
public interface TransactionRepository extends JpaRepository<BankTransaction, Long> {

    // ===== Tra cứu theo mã giao dịch =====
    Optional<BankTransaction> findByTransactionCode(String transactionCode);

    // Dùng khi sinh mã giao dịch mới để tránh trùng
    boolean existsByTransactionCode(String transactionCode);

    // ===== Người dùng: chi tiết 1 giao dịch (chỉ khi liên quan đến tài khoản của họ) =====
    @EntityGraph(attributePaths = { "fromAccount", "toAccount" })
    @Query("""
            select t from BankTransaction t
            where t.id = :id
              and (t.fromAccount.id in :accountIds or t.toAccount.id in :accountIds)
            """)
    Optional<BankTransaction> findByIdAndAccountIds(@Param("id") Long id,
                                                @Param("accountIds") Collection<Long> accountIds);

    // ===== Người dùng: lịch sử giao dịch (lọc + tìm kiếm + phân trang) =====
    // type, status, keyword có thể null = không lọc.
    // keyword truyền vào dạng đã xử lý sẵn: "%abc%" (chữ thường).
    @EntityGraph(attributePaths = { "fromAccount", "toAccount" })
    @Query("""
            select t from BankTransaction t
            where (t.fromAccount.id in :accountIds or t.toAccount.id in :accountIds)
              and (:type is null or t.transactionType = :type)
              and (:status is null or t.status = :status)
              and t.createdAt >= :fromDate
              and t.createdAt < :toDate
              and (:keyword is null
                   or lower(t.transactionCode) like :keyword
                   or lower(t.description) like :keyword)
            """)
    Page<BankTransaction> searchHistory(@Param("accountIds") Collection<Long> accountIds,
                                    @Param("type") TransactionType type,
                                    @Param("status") TransactionStatus status,
                                    @Param("fromDate") LocalDateTime fromDate,
                                    @Param("toDate") LocalDateTime toDate,
                                    @Param("keyword") String keyword,
                                    Pageable pageable);

    // ===== Dashboard người dùng =====

    // "Giao dịch tháng này"
    @Query("""
            select count(t) from BankTransaction t
            where (t.fromAccount.id in :accountIds or t.toAccount.id in :accountIds)
              and t.createdAt >= :fromDate
              and t.createdAt < :toDate
            """)
    long countByAccountIdsAndPeriod(@Param("accountIds") Collection<Long> accountIds,
                                    @Param("fromDate") LocalDateTime fromDate,
                                    @Param("toDate") LocalDateTime toDate);

    // Dữ liệu cho biểu đồ 7 ngày: lấy danh sách rồi gom theo ngày ở service
    @Query("""
            select t from BankTransaction t
            where (t.fromAccount.id in :accountIds or t.toAccount.id in :accountIds)
              and t.status = :status
              and t.createdAt >= :fromDate
              and t.createdAt < :toDate
            order by t.createdAt
            """)
    List<BankTransaction> findForChart(@Param("accountIds") Collection<Long> accountIds,
                                   @Param("status") TransactionStatus status,
                                   @Param("fromDate") LocalDateTime fromDate,
                                   @Param("toDate") LocalDateTime toDate);

    // ===== Admin =====

    // Danh sách giao dịch toàn hệ thống (lọc + phân trang)
    @EntityGraph(attributePaths = { "fromAccount", "fromAccount.user", "toAccount", "toAccount.user" })
    @Query("""
            select t from BankTransaction t
            where (:type is null or t.transactionType = :type)
              and (:status is null or t.status = :status)
              and t.createdAt >= :fromDate
              and t.createdAt < :toDate
              and (:keyword is null
                   or lower(t.transactionCode) like :keyword
                   or lower(t.description) like :keyword)
            """)
    Page<BankTransaction> searchAll(@Param("type") TransactionType type,
                                @Param("status") TransactionStatus status,
                                @Param("fromDate") LocalDateTime fromDate,
                                @Param("toDate") LocalDateTime toDate,
                                @Param("keyword") String keyword,
                                Pageable pageable);

    long countByStatus(TransactionStatus status);

    // "Tổng số tiền giao dịch" (tính riêng từng loại tiền tệ)
    @Query("""
            select coalesce(sum(t.amount), 0) from BankTransaction t
            where t.status = :status and t.currency = :currency
            """)
    BigDecimal sumAmountByStatusAndCurrency(@Param("status") TransactionStatus status,
                                            @Param("currency") String currency);

    // "Thống kê giao dịch theo loại" (biểu đồ tròn): mỗi phần tử là [TransactionType, Long]
    @Query("select t.transactionType, count(t) from BankTransaction t group by t.transactionType")
    List<Object[]> countGroupByType();
}