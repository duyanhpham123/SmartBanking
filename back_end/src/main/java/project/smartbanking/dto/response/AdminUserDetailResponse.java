package project.smartbanking.dto.response;

import java.util.List;

public record AdminUserDetailResponse(
        UserResponse user,
        List<AccountResponse> accounts) {
}