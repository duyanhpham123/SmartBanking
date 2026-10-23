package project.smartbanking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

import project.smartbanking.model.enums.Gender;
import project.smartbanking.model.enums.UserStatus;

// Tuyệt đối không có trường password
public record UserResponse(
        Long id,
        String username,
        String email,
        String phone,
        String fullName,
        LocalDate dateOfBirth,
        Gender gender,
        String avatarUrl,
        UserStatus status,
        boolean emailVerified,
        boolean phoneVerified,
        Set<String> roles,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt) {
}