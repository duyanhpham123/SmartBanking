package project.smartbanking.dto.response;

// Dùng cho các API chỉ cần trả về một câu thông báo (đăng ký, đăng xuất, đổi mật khẩu...)
public record MessageResponse(String message) {
}