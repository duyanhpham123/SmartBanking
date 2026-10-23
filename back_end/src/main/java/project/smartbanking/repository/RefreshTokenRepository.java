package project.smartbanking.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.RefreshToken;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    // Dùng khi người dùng gửi refresh token để xin access token mới
    Optional<RefreshToken> findByToken(String token);

    // Chỉ lấy token còn hiệu lực (chưa thu hồi, chưa hết hạn)
    Optional<RefreshToken> findByTokenAndRevokedFalseAndExpiresAtAfter(String token, LocalDateTime now);

    List<RefreshToken> findByUserIdAndRevokedFalse(Long userId);

    // Thu hồi toàn bộ token của một người dùng
    // (đăng xuất mọi thiết bị, đổi mật khẩu, bị admin khóa tài khoản)
    @Modifying(clearAutomatically = true)
    @Query("update RefreshToken t set t.revoked = true "
            + "where t.user.id = :userId and t.revoked = false")
    int revokeAllByUserId(@Param("userId") Long userId);

    // Dọn token đã hết hạn
    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") LocalDateTime now);
}