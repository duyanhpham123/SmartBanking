package project.smartbanking.dto.response;

import java.util.List;

public record AdminUserDetailResponse(
        UserResponse user,
        CustomerProfileResponse profile,
        List<AccountResponse> accounts,
        List<AuditLogResponse> recentActivities) {
}
