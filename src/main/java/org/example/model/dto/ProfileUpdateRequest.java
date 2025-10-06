package org.example.model.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 255, message = "Имя не должно превышать 255 символов")
    private String displayName;

    @Email(message = "Некорректный формат email")
    private String email;

    private String phoneNumber;

    @Size(max = 255, message = "Отдел не должен превышать 255 символов")
    private String department;

    @Size(max = 255, message = "Должность не должна превышать 255 символов")
    private String position;
}