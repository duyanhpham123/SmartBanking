package project.smartbanking.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.User;
import project.smartbanking.model.enums.UserStatus;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    Optional<User> findByPhone(String phone);

    // Đăng nhập bằng email hoặc số điện thoại: truyền cùng một giá trị vào cả hai tham số
    Optional<User> findByEmailOrPhone(String email, String phone);

    Optional<User> findByUsernameAndStatus(String username, UserStatus status);

    // Kiểm tra trùng khi đăng ký
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);

    // status, keyword có thể null = không lọc. keyword đã xử lý sẵn dạng "%abc%" (chữ thường).
    @Query("""
            select u from User u
            where (:status is null or u.status = :status)
              and (:keyword is null
                   or lower(u.username) like :keyword
                   or lower(u.email) like :keyword
                   or lower(u.fullName) like :keyword
                   or u.phone like :keyword)
            """)
    Page<User> search(@Param("status") UserStatus status,
                      @Param("keyword") String keyword,
                      Pageable pageable);
}