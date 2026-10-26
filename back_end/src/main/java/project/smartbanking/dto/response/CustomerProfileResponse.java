package project.smartbanking.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

import project.smartbanking.model.enums.IdDocumentType;
import project.smartbanking.model.enums.IncomeRange;
import project.smartbanking.model.enums.KycStatus;

// idNumber: phía khách hàng luôn bị che (vd 0791******34), chỉ admin thấy đầy đủ
public record CustomerProfileResponse(
        IdDocumentType idType,
        String idNumber,
        LocalDate idIssueDate,
        String idIssuePlace,
        String nationality,
        String permanentAddress,
        String currentAddress,
        String occupation,
        String employer,
        IncomeRange monthlyIncome,
        KycStatus kycStatus,
        String kycNote,
        LocalDateTime submittedAt,
        LocalDateTime reviewedAt) {
}
