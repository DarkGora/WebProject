package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CallbackRequest {
    @NotBlank(message = "Имя не может быть пустым")
    private String name;

    @NotBlank(message = "Телефон не может быть пустым")
    private String phone;

    private String service;

    private String comment;

    @Email(message = "Некорректный формат email")
    private String email;

    private boolean sendDoc = true;
    private boolean sendExcel = true;

    public String getMessage() {
        return comment;
    }
}