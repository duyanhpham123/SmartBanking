package project.smartbanking.service;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import project.smartbanking.dto.request.RejectKycRequest;
import project.smartbanking.dto.request.UpdateKycRequest;
import project.smartbanking.dto.response.CustomerProfileResponse;
import project.smartbanking.exception.BadRequestException;
import project.smartbanking.exception.ConflictException;
import project.smartbanking.exception.ResourceNotFoundException;
import project.smartbanking.mapper.CustomerProfileMapper;
import project.smartbanking.model.CustomerProfile;
import project.smartbanking.model.User;
import project.smartbanking.model.enums.IdDocumentType;
import project.smartbanking.model.enums.KycStatus;
import project.smartbanking.repository.CustomerProfileRepository;
import project.smartbanking.repository.UserRepository;

@Service
public class CustomerProfileService {

    private static final Pattern CCCD = Pattern.compile("^\\d{12}$");
    private static final Pattern CMND = Pattern.compile("^\\d{9}$");
    private static final Pattern PASSPORT = Pattern.compile("^[A-Z]\\d{7}$");

    private final CustomerProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CustomerProfileMapper profileMapper;
    private final NotificationService notificationService;

    public CustomerProfileService(CustomerProfileRepository profileRepository,
                                  UserRepository userRepository,
                                  CustomerProfileMapper profileMapper,
                                  NotificationService notificationService) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
        this.profileMapper = profileMapper;
        this.notificationService = notificationService;
    }

    // ===================== KHÁCH HÀNG =====================

    // Số giấy tờ luôn bị che khi trả về cho khách hàng
    @Transactional(readOnly = true)
    public CustomerProfileResponse getMyProfile(Long userId) {
        return profileMapper.toResponse(profileRepository.findByUserId(userId).orElse(null), true);
    }

    // Gửi / gửi lại hồ sơ định danh. Sau khi gửi, hồ sơ chuyển về PENDING chờ ngân hàng xác minh.
    // Hồ sơ đã VERIFIED thì khách không tự sửa được nữa (tránh đổi danh tính sau khi đã xác minh).
    @Transactional
    public CustomerProfileResponse submitMyProfile(Long userId, UpdateKycRequest request) {
        CustomerProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> new CustomerProfile(findUser(userId)));

        if (profile.getKycStatus() == KycStatus.VERIFIED) {
            throw new BadRequestException("Hồ sơ đã được xác minh, vui lòng liên hệ ngân hàng để thay đổi");
        }

        String idNumber = normalizeIdNumber(request.idType(), request.idNumber());
        if (profileRepository.existsByIdNumberAndUserIdNot(idNumber, userId)) {
            throw new ConflictException("Số giấy tờ đã được đăng ký bởi khách hàng khác");
        }

        profileMapper.update(profile, request, idNumber);
        profile.setKycStatus(KycStatus.PENDING);
        profile.setKycNote(null);
        profile.setSubmittedAt(LocalDateTime.now());
        profile.setReviewedAt(null);
        profile.setReviewedBy(null);

        return profileMapper.toResponse(profileRepository.save(profile), true);
    }

    // ===================== ADMIN =====================

    // Admin xem đầy đủ số giấy tờ để đối chiếu
    @Transactional(readOnly = true)
    public CustomerProfileResponse getProfileForAdmin(Long userId) {
        return profileMapper.toResponse(profileRepository.findByUserId(userId).orElse(null), false);
    }

    @Transactional
    public CustomerProfileResponse verify(Long adminId, Long userId) {
        CustomerProfile profile = findProfile(userId);
        if (profile.getKycStatus() != KycStatus.PENDING) {
            throw new BadRequestException("Chỉ duyệt được hồ sơ đang chờ xác minh");
        }

        profile.setKycStatus(KycStatus.VERIFIED);
        profile.setKycNote(null);
        markReviewed(profile, adminId);
        notificationService.notifySecurity(userId, "Hồ sơ đã được xác minh",
                "Thông tin định danh của bạn đã được ngân hàng xác minh thành công.");

        return profileMapper.toResponse(profileRepository.save(profile), false);
    }

    // Từ chối hồ sơ đang chờ, hoặc thu hồi xác minh khi phát hiện sai lệch
    @Transactional
    public CustomerProfileResponse reject(Long adminId, Long userId, RejectKycRequest request) {
        CustomerProfile profile = findProfile(userId);
        if (profile.getKycStatus() != KycStatus.PENDING && profile.getKycStatus() != KycStatus.VERIFIED) {
            throw new BadRequestException("Hồ sơ không ở trạng thái có thể từ chối");
        }

        String note = request.note().trim();
        profile.setKycStatus(KycStatus.REJECTED);
        profile.setKycNote(note);
        markReviewed(profile, adminId);
        notificationService.notifySecurity(userId, "Hồ sơ cần bổ sung",
                "Thông tin định danh của bạn chưa được chấp nhận. Lý do: " + note);

        return profileMapper.toResponse(profileRepository.save(profile), false);
    }

    // ===================== HÀM PHỤ =====================

    private String normalizeIdNumber(IdDocumentType type, String raw) {
        String value = raw.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        boolean valid = switch (type) {
            case CCCD -> CCCD.matcher(value).matches();
            case CMND -> CMND.matcher(value).matches();
            case PASSPORT -> PASSPORT.matcher(value).matches();
        };
        if (!valid) {
            throw new BadRequestException(switch (type) {
                case CCCD -> "Số CCCD phải gồm 12 chữ số";
                case CMND -> "Số CMND phải gồm 9 chữ số";
                case PASSPORT -> "Số hộ chiếu gồm 1 chữ cái và 7 chữ số";
            });
        }
        return value;
    }

    private void markReviewed(CustomerProfile profile, Long adminId) {
        profile.setReviewedAt(LocalDateTime.now());
        profile.setReviewedBy(adminId);
    }

    private CustomerProfile findProfile(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Khách hàng chưa gửi hồ sơ định danh"));
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng"));
    }
}
