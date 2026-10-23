package project.smartbanking.util;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

public final class AuditContext {

    public static final String USER_ID = "audit.userId";
    public static final String ENTITY_TYPE = "audit.entityType";
    public static final String ENTITY_ID = "audit.entityId";

    private AuditContext() {
    }

    // Dùng khi người dùng chưa đăng nhập lúc gửi yêu cầu (đăng ký, đăng nhập, đặt lại mật khẩu)
    public static void setUserId(Long userId) {
        set(USER_ID, userId);
    }

    // Đối tượng bị tác động, ví dụ ("TRANSACTION", 27)
    public static void setEntity(String type, Object id) {
        set(ENTITY_TYPE, type);
        set(ENTITY_ID, String.valueOf(id));
    }

    private static void set(String key, Object value) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(key, value, RequestAttributes.SCOPE_REQUEST);
        }
    }
}