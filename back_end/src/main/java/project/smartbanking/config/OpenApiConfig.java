package project.smartbanking.config;

import static java.util.Map.entry;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;

@Configuration
public class OpenApiConfig {

    // Nhóm API: thứ tự ở đây cũng là thứ tự hiển thị trên Swagger
    private record TagRule(String prefix, String name, String description) {
    }

    private static final List<TagRule> TAG_RULES = List.of(
            new TagRule("/api/v1/auth", "Xác thực", "Đăng ký, đăng nhập, làm mới token, đăng xuất, quên và đặt lại mật khẩu"),
            new TagRule("/api/v1/users", "Người dùng", "Hồ sơ cá nhân và đổi mật khẩu"),
            new TagRule("/api/v1/accounts", "Tài khoản ngân hàng", "Tài khoản của tôi, tra cứu người nhận, lịch sử theo tài khoản"),
            new TagRule("/api/v1/transactions", "Giao dịch", "Nạp, rút, chuyển tiền, lịch sử và xuất file"),
            new TagRule("/api/v1/payments", "Thanh toán hóa đơn", "Thanh toán điện, nước, internet, điện thoại"),
            new TagRule("/api/v1/otp", "Mã OTP", "Gửi và kiểm tra mã xác thực một lần"),
            new TagRule("/api/v1/notifications", "Thông báo", "Danh sách, số chưa đọc, đánh dấu đã đọc"),
            new TagRule("/api/v1/dashboard", "Dashboard", "Số liệu trang chủ của người dùng"),
            new TagRule("/api/v1/admin/dashboard", "Admin - Dashboard", "Số liệu tổng quan toàn hệ thống"),
            new TagRule("/api/v1/admin/users", "Admin - Người dùng", "Xem, khóa và mở khóa người dùng"),
            new TagRule("/api/v1/admin/accounts", "Admin - Tài khoản", "Xem, khóa và mở khóa tài khoản ngân hàng"),
            new TagRule("/api/v1/admin/transactions", "Admin - Giao dịch", "Xem giao dịch toàn hệ thống"),
            new TagRule("/api/v1/admin/audit-logs", "Admin - Nhật ký", "Nhật ký thao tác của hệ thống"));

    // Tiêu đề tiếng Việt cho từng API. Có cả hai dạng đường dẫn lịch sử giao dịch (có hoặc không có /history)
    private static final Map<String, String> SUMMARIES = Map.ofEntries(
            // Xác thực
            entry("POST /api/v1/auth/register", "Đăng ký tài khoản"),
            entry("POST /api/v1/auth/login", "Đăng nhập (email hoặc số điện thoại)"),
            entry("POST /api/v1/auth/refresh", "Làm mới access token"),
            entry("POST /api/v1/auth/logout", "Đăng xuất (thu hồi refresh token)"),
            entry("POST /api/v1/auth/logout-all", "Đăng xuất khỏi mọi thiết bị"),
            entry("POST /api/v1/auth/forgot-password", "Quên mật khẩu: gửi mã OTP"),
            entry("POST /api/v1/auth/reset-password", "Đặt lại mật khẩu bằng OTP"),
            // Người dùng
            entry("GET /api/v1/users/me", "Xem hồ sơ của tôi"),
            entry("PUT /api/v1/users/me", "Cập nhật hồ sơ"),
            entry("PUT /api/v1/users/me/password", "Đổi mật khẩu"),
            // Tài khoản
            entry("GET /api/v1/accounts", "Danh sách tài khoản của tôi"),
            entry("POST /api/v1/accounts", "Mở tài khoản mới"),
            entry("GET /api/v1/accounts/lookup", "Tra cứu người nhận theo số tài khoản"),
            entry("GET /api/v1/accounts/{id}", "Chi tiết một tài khoản"),
            entry("GET /api/v1/accounts/{id}/transactions", "Lịch sử giao dịch của một tài khoản"),
            entry("PUT /api/v1/accounts/{id}/name", "Đổi tên tài khoản"),
            // Giao dịch
            entry("POST /api/v1/transactions/deposit", "Nạp tiền"),
            entry("POST /api/v1/transactions/withdraw", "Rút tiền"),
            entry("POST /api/v1/transactions/transfer", "Chuyển tiền"),
            entry("GET /api/v1/transactions", "Lịch sử giao dịch (lọc, tìm kiếm, phân trang)"),
            entry("GET /api/v1/transactions/history", "Lịch sử giao dịch (lọc, tìm kiếm, phân trang)"),
            entry("GET /api/v1/transactions/export", "Xuất lịch sử giao dịch ra CSV"),
            entry("GET /api/v1/transactions/history/export", "Xuất lịch sử giao dịch ra CSV"),
            entry("GET /api/v1/transactions/{id}", "Chi tiết một giao dịch"),
            // Thanh toán, OTP
            entry("POST /api/v1/payments", "Thanh toán hóa đơn"),
            entry("POST /api/v1/otp/send", "Gửi mã OTP"),
            entry("POST /api/v1/otp/verify", "Kiểm tra mã OTP (không tiêu hủy mã)"),
            // Thông báo
            entry("GET /api/v1/notifications", "Danh sách thông báo"),
            entry("GET /api/v1/notifications/unread-count", "Số thông báo chưa đọc"),
            entry("PUT /api/v1/notifications/{id}/read", "Đánh dấu một thông báo đã đọc"),
            entry("PUT /api/v1/notifications/read-all", "Đánh dấu tất cả đã đọc"),
            // Dashboard
            entry("GET /api/v1/dashboard/summary", "Các thẻ tổng quan trang chủ"),
            entry("GET /api/v1/dashboard/chart", "Biểu đồ giao dịch theo ngày"),
            entry("GET /api/v1/dashboard/recent-transactions", "Giao dịch gần đây"),
            // Admin
            entry("GET /api/v1/admin/dashboard/overview", "Tổng quan hệ thống"),
            entry("GET /api/v1/admin/dashboard/transactions-by-type", "Thống kê giao dịch theo loại"),
            entry("GET /api/v1/admin/dashboard/recent-transactions", "Giao dịch gần đây (toàn hệ thống)"),
            entry("GET /api/v1/admin/dashboard/chart", "Biểu đồ giao dịch toàn hệ thống theo ngày"),
            entry("GET /api/v1/admin/users", "Danh sách người dùng"),
            entry("GET /api/v1/admin/users/{id}", "Chi tiết người dùng và tài khoản của họ"),
            entry("PUT /api/v1/admin/users/{id}/lock", "Khóa người dùng"),
            entry("PUT /api/v1/admin/users/{id}/unlock", "Mở khóa người dùng"),
            entry("GET /api/v1/admin/accounts", "Danh sách tài khoản ngân hàng"),
            entry("GET /api/v1/admin/accounts/{id}", "Chi tiết tài khoản ngân hàng"),
            entry("PUT /api/v1/admin/accounts/{id}/lock", "Khóa tài khoản ngân hàng"),
            entry("PUT /api/v1/admin/accounts/{id}/unlock", "Mở khóa tài khoản ngân hàng"),
            entry("GET /api/v1/admin/transactions", "Danh sách giao dịch toàn hệ thống"),
            entry("GET /api/v1/admin/transactions/{id}", "Chi tiết giao dịch"),
            entry("GET /api/v1/admin/audit-logs", "Danh sách nhật ký"),
            entry("GET /api/v1/admin/audit-logs/{id}", "Chi tiết một dòng nhật ký"));

    // Hướng dẫn dùng cho các API cần lưu ý (hỗ trợ Markdown)
    private static final Map<String, String> DESCRIPTIONS = Map.ofEntries(
            entry("POST /api/v1/auth/login",
                    "`identifier` là email hoặc số điện thoại. Kết quả có `accessToken` (dán vào nút **Authorize**, "
                            + "**không** gõ chữ Bearer) và `refreshToken`."),
            entry("POST /api/v1/auth/refresh",
                    "Mỗi refresh token chỉ dùng được một lần; thành công sẽ nhận cặp token mới."),
            entry("POST /api/v1/auth/logout-all",
                    "Cần gửi kèm access token. Thu hồi mọi refresh token; access token hiện tại vẫn dùng được đến khi hết hạn."),
            entry("GET /api/v1/accounts/lookup",
                    "Trả về tên chủ tài khoản để xác nhận trước khi chuyển tiền. Không trả số dư."),
            entry("POST /api/v1/otp/send",
                    "Hỗ trợ mục đích `TRANSFER` và `CHANGE_PASSWORD`. Tối đa 3 lần trong 5 phút. "
                            + "Khi `app.otp.expose-in-response=true`, mã được trả về trong `devOtp` (chỉ dùng khi phát triển)."),
            entry("POST /api/v1/transactions/transfer",
                    "**Trước khi gọi:** (1) `POST /otp/send` với `{\"purpose\":\"TRANSFER\"}` để lấy mã OTP "
                            + "(hiệu lực 5 phút, dùng một lần); (2) tạo một `Idempotency-Key` **mới** cho mỗi lần chuyển. "
                            + "Gửi lại đúng mã sẽ nhận lại kết quả cũ (header `Idempotent-Replayed: true`), không chuyển lần nữa. "
                            + "Chỉ chuyển giữa hai tài khoản cùng loại tiền tệ."),
            entry("POST /api/v1/payments",
                    "Cần OTP mục đích `TRANSFER` và `Idempotency-Key` mới cho mỗi lần thanh toán, giống chuyển tiền. "
                            + "Chỉ thanh toán bằng tài khoản VND."),
            entry("GET /api/v1/transactions/export",
                    "Trả về file CSV (UTF-8), tối đa 5000 giao dịch mới nhất theo bộ lọc. Ngày nhập dạng `yyyy-MM-dd`."),
            entry("GET /api/v1/transactions/history/export",
                    "Trả về file CSV (UTF-8), tối đa 5000 giao dịch mới nhất theo bộ lọc. Ngày nhập dạng `yyyy-MM-dd`."));

    // API công khai: không cần đăng nhập, không hiện ổ khóa
    private static final Set<String> PUBLIC_ENDPOINTS = Set.of(
            "POST /api/v1/auth/register",
            "POST /api/v1/auth/login",
            "POST /api/v1/auth/refresh",
            "POST /api/v1/auth/logout",
            "POST /api/v1/auth/forgot-password",
            "POST /api/v1/auth/reset-password");

    // API trả 201 khi thành công
    private static final Set<String> CREATED_ENDPOINTS = Set.of(
            "POST /api/v1/auth/register",
            "POST /api/v1/accounts",
            "POST /api/v1/transactions/deposit",
            "POST /api/v1/transactions/withdraw",
            "POST /api/v1/transactions/transfer",
            "POST /api/v1/payments");

    private static final Set<String> CONFLICT_ENDPOINTS = Set.of(
            "POST /api/v1/auth/register",
            "POST /api/v1/transactions/transfer",
            "POST /api/v1/payments");

    private static final Set<String> RATE_LIMITED_ENDPOINTS = Set.of(
            "POST /api/v1/auth/forgot-password",
            "POST /api/v1/auth/reset-password",
            "POST /api/v1/otp/send",
            "POST /api/v1/otp/verify",
            "POST /api/v1/transactions/transfer",
            "POST /api/v1/payments");

    // API có thể trả 404 dù đường dẫn không có biến {id} (không tìm thấy tài khoản)
    private static final Set<String> NOT_FOUND_ENDPOINTS = Set.of(
            "GET /api/v1/accounts/lookup",
            "POST /api/v1/transactions/deposit",
            "POST /api/v1/transactions/withdraw",
            "POST /api/v1/transactions/transfer",
            "POST /api/v1/payments");

    @Bean
    public OpenAPI smartBankingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("SmartBanking API")
                        .version("1.0")
                        .description("""
                                API ngân hàng số SmartBanking: tài khoản, nạp/rút/chuyển tiền, thanh toán hóa đơn, \
                                OTP, thông báo, dashboard và quản trị.

                                **Cách thử nhanh**
                                1. Mở nhóm **Xác thực**, gọi `POST /auth/login`, copy `accessToken` (không lấy dấu nháy).
                                2. Bấm nút **Authorize** ở đầu trang, dán token, bấm Authorize rồi Close.
                                3. Các API còn lại (có ổ khóa) từ giờ tự gửi kèm token. Token sống 15 phút, hết hạn thì đăng nhập lại.
                                """)
                        .contact(new Contact().name("SmartBanking")))
                .components(new Components().addSecuritySchemes("bearerAuth", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Dán accessToken (không gõ chữ Bearer)")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }

    @Bean
    public OpenApiCustomizer smartBankingCustomizer() {
        return openApi -> {
            registerErrorSchema(openApi);

            // Nhóm API theo thứ tự đã khai báo
            openApi.setTags(TAG_RULES.stream()
                    .map(r -> new Tag().name(r.name()).description(r.description()))
                    .toList());

            openApi.getPaths().forEach((path, item) ->
                    item.readOperationsMap().forEach((method, operation) -> customize(path, method, operation)));
        };
    }

    private void customize(String path, PathItem.HttpMethod method, Operation op) {
        String key = method.name() + " " + path;
        boolean isPublic = PUBLIC_ENDPOINTS.contains(key);

        op.setTags(List.of(tagNameOf(path)));

        String summary = SUMMARIES.get(key);
        if (summary != null) {
            op.setSummary(summary);
        }
        String description = DESCRIPTIONS.get(key);
        if (description != null) {
            op.setDescription(description);
        }

        // Danh sách rỗng nghĩa là API này không yêu cầu đăng nhập (xóa ổ khóa)
        if (isPublic) {
            op.setSecurity(List.of());
        }

        if (CREATED_ENDPOINTS.contains(key)) {
            markAsCreated(op);
        }
        if (key.endsWith("/export")) {
            ApiResponse ok = op.getResponses().get("200");
            if (ok != null) {
                ok.setDescription("File CSV (UTF-8)");
            }
        }

        describeIdempotencyHeader(op);

        // Các mã lỗi hay gặp
        boolean hasInput = op.getRequestBody() != null
                || (op.getParameters() != null && !op.getParameters().isEmpty());
        if (hasInput) {
            addError(op, "400", "Dữ liệu không hợp lệ");
        }
        if (!isPublic) {
            addError(op, "401", "Chưa đăng nhập hoặc phiên đăng nhập đã hết hạn");
        }
        if ("POST /api/v1/auth/login".equals(key)) {
            addError(op, "401", "Sai tài khoản hoặc mật khẩu");
            addError(op, "403", "Tài khoản bị khóa hoặc chưa được kích hoạt");
        }
        if ("POST /api/v1/auth/refresh".equals(key)) {
            addError(op, "401", "Refresh token không hợp lệ hoặc đã hết hạn");
        }
        if (path.startsWith("/api/v1/admin")) {
            addError(op, "403", "Không đủ quyền (chỉ quản trị viên)");
        }
        if (path.contains("{") || NOT_FOUND_ENDPOINTS.contains(key)) {
            addError(op, "404", "Không tìm thấy dữ liệu");
        }
        if (CONFLICT_ENDPOINTS.contains(key)) {
            addError(op, "409", "Dữ liệu bị trùng hoặc Idempotency-Key đã dùng cho yêu cầu khác");
        }
        if (RATE_LIMITED_ENDPOINTS.contains(key)) {
            addError(op, "429", "Thao tác quá nhiều lần, vui lòng thử lại sau");
        }
    }

    private String tagNameOf(String path) {
        return TAG_RULES.stream()
                .filter(r -> path.startsWith(r.prefix()))
                .map(TagRule::name)
                .findFirst()
                .orElse("Khác");
    }

    // Đổi phản hồi "200" mặc định thành "201 Tạo thành công" (giữ nguyên nội dung mẫu)
    private void markAsCreated(Operation op) {
        ApiResponses current = op.getResponses();
        ApiResponse ok = current.remove("200");
        if (ok == null) {
            return;
        }
        ok.setDescription("Tạo thành công");
        ApiResponses reordered = new ApiResponses();
        reordered.addApiResponse("201", ok);
        reordered.putAll(current);
        op.setResponses(reordered);
    }

    private void describeIdempotencyHeader(Operation op) {
        if (op.getParameters() == null) {
            return;
        }
        for (Parameter p : op.getParameters()) {
            if ("Idempotency-Key".equalsIgnoreCase(p.getName())) {
                p.setDescription("Mã duy nhất cho **mỗi lần** giao dịch (8-100 ký tự: chữ, số, `-` hoặc `_`). "
                        + "Mỗi lần mới phải gõ mã khác; gửi lại cùng mã sẽ nhận lại kết quả cũ.");
            }
        }
    }

    private void addError(Operation op, String code, String description) {
        if (op.getResponses().containsKey(code)) {
            return;
        }
        Content content = new Content().addMediaType("application/json",
                new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiErrorResponse")));
        op.getResponses().addApiResponse(code, new ApiResponse().description(description).content(content));
    }

    // Cấu trúc JSON lỗi (khớp với ApiErrorResponse)
    private void registerErrorSchema(OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        Schema<?> error = new Schema<>()
                .type("object")
                .addProperty("timestamp", new Schema<>().type("string").format("date-time"))
                .addProperty("status", new Schema<>().type("integer").format("int32").example(400))
                .addProperty("error", new Schema<>().type("string").example("Bad Request"))
                .addProperty("message", new Schema<>().type("string").example("Số dư không đủ để thực hiện giao dịch"))
                .addProperty("path", new Schema<>().type("string").example("/api/v1/transactions/transfer"))
                .addProperty("errors", new Schema<>().type("object")
                        .additionalProperties(new Schema<>().type("string"))
                        .description("Lỗi theo từng trường (chỉ có khi dữ liệu không hợp lệ)"));
        openApi.getComponents().addSchemas("ApiErrorResponse", error);
    }
}