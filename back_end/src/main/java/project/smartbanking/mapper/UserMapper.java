package project.smartbanking.mapper;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import project.smartbanking.dto.request.UpdateProfileRequest;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.model.Role;
import project.smartbanking.model.User;

@Component
public class UserMapper {

    // User (entity) -> UserResponse (trả về cho React). Không đưa password ra ngoài.
    public UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }

        Set<String> roles = user.getRoles() == null
                ? Collections.emptySet()
                : user.getRoles().stream()
                        .filter(role -> role != null && role.getName() != null)
                        .map(Role::getName)
                        .collect(Collectors.toSet());

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhone(),
                user.getFullName(),
                user.getDateOfBirth(),
                user.getGender(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.isEmailVerified(),
                user.isPhoneVerified(),
                roles,
                user.getLastLoginAt(),
                user.getCreatedAt());
    }

    // Cập nhật hồ sơ: chỉ ghi đè các trường được phép sửa, không đụng email/phone/password/status
    public void updateProfile(User user, UpdateProfileRequest request) {
        user.setFullName(request.fullName());
        user.setDateOfBirth(request.dateOfBirth());
        user.setGender(request.gender());
        user.setAvatarUrl(request.avatarUrl());
    }
}