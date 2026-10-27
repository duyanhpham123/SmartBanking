package project.smartbanking.mapper;

import org.springframework.stereotype.Component;

import project.smartbanking.dto.request.UpdateKycRequest;
import project.smartbanking.dto.response.CustomerProfileResponse;
import project.smartbanking.model.CustomerProfile;
import project.smartbanking.model.enums.KycStatus;

@Component
public class CustomerProfileMapper {

    // Khách hàng chưa khai báo: trả về hồ sơ rỗng với trạng thái NOT_SUBMITTED thay vì null
    public CustomerProfileResponse toResponse(CustomerProfile p, boolean maskIdNumber) {
        if (p == null) {
            return new CustomerProfileResponse(null, null, null, null, null, null, null, null, null, null,
                    KycStatus.NOT_SUBMITTED, null, null, null);
        }
        return new CustomerProfileResponse(
                p.getIdType(),
                maskIdNumber ? mask(p.getIdNumber()) : p.getIdNumber(),
                p.getIdIssueDate(),
                p.getIdIssuePlace(),
                p.getNationality(),
                p.getPermanentAddress(),
                p.getCurrentAddress(),
                p.getOccupation(),
                p.getEmployer(),
                p.getMonthlyIncome(),
                p.getKycStatus(),
                p.getKycNote(),
                p.getSubmittedAt(),
                p.getReviewedAt());
    }

    public void update(CustomerProfile p, UpdateKycRequest request, String normalizedIdNumber) {
        p.setIdType(request.idType());
        p.setIdNumber(normalizedIdNumber);
        p.setIdIssueDate(request.idIssueDate());
        p.setIdIssuePlace(request.idIssuePlace().trim());
        p.setNationality(request.nationality().trim());
        p.setPermanentAddress(request.permanentAddress().trim());
        p.setCurrentAddress(blankToNull(request.currentAddress()));
        p.setOccupation(blankToNull(request.occupation()));
        p.setEmployer(blankToNull(request.employer()));
        p.setMonthlyIncome(request.monthlyIncome());
    }

    // Giữ 4 ký tự đầu và 2 ký tự cuối: 079123456789 -> 0791******89
    private String mask(String value) {
        if (value == null || value.length() <= 6) {
            return value;
        }
        return value.substring(0, 4) + "*".repeat(value.length() - 6) + value.substring(value.length() - 2);
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}
