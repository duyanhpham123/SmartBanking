package project.smartbanking.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import project.smartbanking.security.UserPrincipal;
import project.smartbanking.service.AuditLogService;
import project.smartbanking.util.AuditContext;

@Component
public class AuditLogFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(AuditLogFilter.class);

    private static final int BODY_CACHE_LIMIT = 16 * 1024;

    // Tên trường (viết thường) mà giá trị luôn bị che trước khi lưu
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "currentpassword", "newpassword", "otp", "code",
            "token", "accesstoken", "refreshtoken", "idnumber");

    private record Rule(String method, Pattern path, String action, String entityType, String label) {
        static Rule of(String method, String regex, String action, String entityType, String label) {
            return new Rule(method, Pattern.compile(regex), action, entityType, label);
        }
    }

    private static final List<Rule> RULES = List.of(
            Rule.of("POST", "^/api/v1/auth/login$", "LOGIN", null, "Đăng nhập"),
            Rule.of("POST", "^/api/v1/auth/register$", "REGISTER", "USER", "Đăng ký tài khoản"),
            Rule.of("POST", "^/api/v1/auth/logout$", "LOGOUT", null, "Đăng xuất"),
            Rule.of("POST", "^/api/v1/auth/logout-all$", "LOGOUT_ALL", null, "Đăng xuất mọi thiết bị"),
            Rule.of("POST", "^/api/v1/auth/forgot-password$", "FORGOT_PASSWORD", null, "Yêu cầu quên mật khẩu"),
            Rule.of("POST", "^/api/v1/auth/reset-password$", "RESET_PASSWORD", "USER", "Đặt lại mật khẩu"),
            Rule.of("PUT", "^/api/v1/users/me$", "UPDATE_PROFILE", "USER", "Cập nhật hồ sơ"),
            Rule.of("PUT", "^/api/v1/users/me/password$", "CHANGE_PASSWORD", "USER", "Đổi mật khẩu"),
            Rule.of("PUT", "^/api/v1/users/me/kyc$", "SUBMIT_KYC", "USER", "Gửi hồ sơ định danh"),
            Rule.of("POST", "^/api/v1/accounts$", "CREATE_ACCOUNT", "ACCOUNT", "Mở tài khoản ngân hàng"),
            Rule.of("PUT", "^/api/v1/accounts/(\\d+)/name$", "RENAME_ACCOUNT", "ACCOUNT", "Đổi tên tài khoản"),
            Rule.of("POST", "^/api/v1/transactions/deposit$", "DEPOSIT", "TRANSACTION", "Nạp tiền"),
            Rule.of("POST", "^/api/v1/transactions/withdraw$", "WITHDRAW", "TRANSACTION", "Rút tiền"),
            Rule.of("POST", "^/api/v1/transactions/transfer$", "TRANSFER", "TRANSACTION", "Chuyển tiền"),
            Rule.of("POST", "^/api/v1/payments$", "PAYMENT", "TRANSACTION", "Thanh toán hóa đơn"),
            Rule.of("POST", "^/api/v1/otp/send$", "OTP_SEND", "OTP", "Gửi mã OTP"),
            Rule.of("POST", "^/api/v1/otp/verify$", "OTP_VERIFY", "OTP", "Kiểm tra mã OTP"),
            Rule.of("PUT", "^/api/v1/admin/users/(\\d+)/lock$", "ADMIN_LOCK_USER", "USER", "Khóa người dùng"),
            Rule.of("PUT", "^/api/v1/admin/users/(\\d+)/unlock$", "ADMIN_UNLOCK_USER", "USER", "Mở khóa người dùng"),
            Rule.of("PUT", "^/api/v1/admin/users/(\\d+)/kyc/verify$", "ADMIN_VERIFY_KYC", "USER", "Duyệt hồ sơ định danh"),
            Rule.of("PUT", "^/api/v1/admin/users/(\\d+)/kyc/reject$", "ADMIN_REJECT_KYC", "USER", "Từ chối hồ sơ định danh"),
            Rule.of("PUT", "^/api/v1/admin/accounts/(\\d+)/lock$", "ADMIN_LOCK_ACCOUNT", "ACCOUNT", "Khóa tài khoản ngân hàng"),
            Rule.of("PUT", "^/api/v1/admin/accounts/(\\d+)/unlock$", "ADMIN_UNLOCK_ACCOUNT", "ACCOUNT", "Mở khóa tài khoản ngân hàng"));

    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AuditLogFilter(AuditLogService auditLogService, ObjectMapper objectMapper) {
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    // Chỉ ghi các yêu cầu thay đổi dữ liệu vào API; bỏ qua yêu cầu đọc, refresh token và thông báo
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();
        if (!uri.startsWith("/api/v1/")) {
            return true;
        }
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)
                || "OPTIONS".equalsIgnoreCase(method)) {
            return true;
        }
        return uri.equals("/api/v1/auth/refresh") || uri.startsWith("/api/v1/notifications");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingRequestWrapper wrapped = new ContentCachingRequestWrapper(request, BODY_CACHE_LIMIT);
        Long principalId = currentUserId();

        try {
            filterChain.doFilter(wrapped, response);
        } finally {
            try {
                writeLog(wrapped, response, principalId);
            } catch (Exception ex) {
                // Lỗi ghi nhật ký không được làm hỏng yêu cầu chính
                log.error("Không ghi được audit log cho {} {}", request.getMethod(), request.getRequestURI(), ex);
            }
        }
    }

    private void writeLog(ContentCachingRequestWrapper request, HttpServletResponse response, Long principalId) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        int status = response.getStatus();
        boolean success = status < 400;

        String action;
        String entityType = null;
        String entityId = null;
        String label;

        Rule rule = findRule(method, uri);
        if (rule != null) {
            action = rule.action();
            entityType = rule.entityType();
            label = rule.label();
            Matcher m = rule.path().matcher(uri);
            if (m.matches() && m.groupCount() >= 1) {
                entityId = m.group(1);
            }
        } else if (uri.startsWith("/api/v1/admin/")) {
            action = "ADMIN_" + method;
            entityType = "ADMIN";
            label = "Thao tác quản trị " + method + " " + uri;
        } else {
            action = "API_" + method;
            label = method + " " + uri;
        }

        if ("LOGIN".equals(action) && !success) {
            action = "LOGIN_FAILED";
        }

        // Người dùng: ưu tiên giá trị service gắn vào (đăng nhập, đăng ký), nếu không thì lấy từ token
        Long userId = principalId;
        if (request.getAttribute(AuditContext.USER_ID) instanceof Long id) {
            userId = id;
        }

        // Đối tượng: ưu tiên giá trị service gắn vào (ví dụ mã giao dịch vừa tạo)
        Object attrType = request.getAttribute(AuditContext.ENTITY_TYPE);
        Object attrId = request.getAttribute(AuditContext.ENTITY_ID);
        if (attrType != null && attrId != null) {
            entityType = attrType.toString();
            entityId = attrId.toString();
        } else if ("USER".equals(entityType) && entityId == null && userId != null) {
            entityId = String.valueOf(userId);
        }

        String description = label + (success ? " - thành công" : " - thất bại (HTTP " + status + ")");

        auditLogService.record(userId, action, entityType, entityId,
                request.getRemoteAddr(), request.getHeader("User-Agent"),
                method, uri, sanitizedBody(request), status, description);
    }

    private Rule findRule(String method, String uri) {
        for (Rule rule : RULES) {
            if (rule.method().equalsIgnoreCase(method) && rule.path().matcher(uri).matches()) {
                return rule;
            }
        }
        return null;
    }

    private Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    // Trả về JSON đã che dữ liệu nhạy cảm, hoặc null nếu không có nội dung / không phải JSON / quá lớn
    private String sanitizedBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        String contentType = request.getContentType();
        if (content.length == 0 || contentType == null || !contentType.toLowerCase().contains("json")) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(content);
            redact(node);
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            return null;
        }
    }

    private void redact(JsonNode node) {
        if (node.isObject()) {
            ObjectNode object = (ObjectNode) node;
            List<String> names = new ArrayList<>();
            object.fieldNames().forEachRemaining(names::add);
            for (String name : names) {
                if (SENSITIVE_KEYS.contains(name.toLowerCase())) {
                    object.put(name, "***");
                } else {
                    redact(object.get(name));
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::redact);
        }
    }
}