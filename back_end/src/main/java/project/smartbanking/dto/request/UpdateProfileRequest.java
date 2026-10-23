package project.smartbanking.dto.request;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import project.smartbanking.model.enums.Gender;

public record UpdateProfileRequest(
        @NotBlank(message = "Họ tên không được để trống")
        @Size(max = 150, message = "Họ tên tối đa 150 ký tự")
        String fullName,

        @Past(message = "Ngày sinh phải là một ngày trong quá khứ")
        LocalDate dateOfBirth,

        Gender gender,

        @Size(max = 500, message = "Đường dẫn ảnh tối đa 500 ký tự")
        String avatarUrl) {
}