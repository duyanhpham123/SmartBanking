package project.smartbanking.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.OtpCode;
import project.smartbanking.model.enums.OtpPurpose;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    // Xác thực: đúng người, đúng mục đích, đúng mã, chưa dùng, chưa hết hạn
    Optional<OtpCode> findFirstByUserIdAndPurposeAndCodeAndUsedFalseAndExpiresAtAfter(
            Long userId, OtpPurpose purpose, String code, LocalDateTime now);

    // Lấy OTP mới nhất còn hiệu lực của người dùng cho một mục đích
    Optional<OtpCode> findFirstByUserIdAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            Long userId, OtpPurpose purpose);

    // Vô hiệu hóa các OTP cũ chưa dùng trước khi sinh OTP mới
    @Modifying(clearAutomatically = true)
    @Query("update OtpCode o set o.used = true "
            + "where o.user.id = :userId and o.purpose = :purpose and o.used = false")
    int invalidateAllByUserIdAndPurpose(@Param("userId") Long userId,
                                        @Param("purpose") OtpPurpose purpose);

    // Giới hạn số lần gửi OTP trong một khoảng thời gian (chống spam)
    long countByUserIdAndPurposeAndCreatedAtAfter(Long userId, OtpPurpose purpose, LocalDateTime since);

    // Dọn OTP đã hết hạn
    @Modifying
    @Query("delete from OtpCode o where o.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
    
    // Tiêu hủy OTP một cách nguyên tử: trả về 1 nếu thành công, 0 nếu mã đã bị dùng trước đó
    @Modifying
    @Query("update OtpCode o set o.used = true where o.id = :id and o.used = false")
    int markUsed(@Param("id") Long id);
}