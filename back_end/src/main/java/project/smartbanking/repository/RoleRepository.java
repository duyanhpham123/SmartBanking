package project.smartbanking.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import project.smartbanking.model.Role;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    // Tìm role theo tên, ví dụ "ROLE_USER" hoặc "ROLE_ADMIN"
    Optional<Role> findByName(String name);

    boolean existsByName(String name);
}