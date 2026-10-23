package project.smartbanking.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.request.ChangePasswordRequest;
import project.smartbanking.dto.request.UpdateProfileRequest;
import project.smartbanking.dto.response.MessageResponse;
import project.smartbanking.dto.response.UserResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.UserMapper;
import project.smartbanking.model.User;
import project.smartbanking.repository.RefreshTokenRepository;
import project.smartbanking.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository,
                       PasswordEncoder passwordEncoder,
                       UserMapper userMapper) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.userMapper = userMapper;
    }

    // ===================== XEM HỒ SƠ =====================
    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return userMapper.toResponse(findUser(userId));
    }

    // ===================== CẬP NHẬT HỒ SƠ =====================
    // Chỉ sửa họ tên, ngày sinh, giới tính, ảnh đại diện. Email, số điện thoại, trạng thái, quyền không sửa ở đây.
    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        userMapper.updateProfile(user, request);
        return userMapper.toResponse(userRepository.save(user));
    }

    // ===================== ĐỔI MẬT KHẨU =====================
    @Transactional
    public MessageResponse changePassword(Long userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        // Dùng 400 (không phải 401) để giao diện không tự đăng xuất người dùng khi nhập sai
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu hiện tại không đúng");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new BadRequestException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.saveAndFlush(user);                      // ghi trước, vì lệnh dưới sẽ làm sạch bộ nhớ đệm
        refreshTokenRepository.revokeAllByUserId(userId);       // các thiết bị khác phải đăng nhập lại

        return new MessageResponse("Đổi mật khẩu thành công, vui lòng đăng nhập lại");
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }
}