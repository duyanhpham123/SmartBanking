package project.smartbanking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.CustomerProfile;

@Repository
public interface CustomerProfileRepository extends JpaRepository<CustomerProfile, Long> {

    Optional<CustomerProfile> findByUserId(Long userId);

    // Một số giấy tờ chỉ được gắn với một khách hàng
    boolean existsByIdNumberAndUserIdNot(String idNumber, Long userId);
}
