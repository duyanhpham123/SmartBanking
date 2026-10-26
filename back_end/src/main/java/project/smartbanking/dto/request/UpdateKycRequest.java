package project.smartbanking.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import project.smartbanking.model.enums.IdDocumentType;
import project.smartbanking.model.enums.IncomeRange;

// Định dạng số giấy tờ được kiểm tra theo idType trong CustomerProfileService
public record UpdateKycRequest(
        @NotNull(message = "Vui lòng chọn loại giấy tờ")
        IdDocumentType idType,

        @NotBlank(message = "Số giấy tờ không được để trống")
        @Size(max = 20, message = "Số giấy tờ tối đa 20 ký tự")
        String idNumber,

        @NotNull(message = "Ngày cấp không được để trống")
        @PastOrPresent(message = "Ngày cấp không được ở tương lai")
        LocalDate idIssueDate,

        @NotBlank(message = "Nơi cấp không được để trống")
        @Size(max = 150, message = "Nơi cấp tối đa 150 ký tự")
        String idIssuePlace,

        @NotBlank(message = "Quốc tịch không được để trống")
        @Size(max = 60, message = "Quốc tịch tối đa 60 ký tự")
        String nationality,

        @NotBlank(message = "Địa chỉ thường trú không được để trống")
        @Size(max = 255, message = "Địa chỉ thường trú tối đa 255 ký tự")
        String permanentAddress,

        @Size(max = 255, message = "Địa chỉ hiện tại tối đa 255 ký tự")
        String currentAddress,

        @Size(max = 100, message = "Nghề nghiệp tối đa 100 ký tự")
        String occupation,

        @Size(max = 150, message = "Nơi làm việc tối đa 150 ký tự")
        String employer,

        IncomeRange monthlyIncome) {
}
