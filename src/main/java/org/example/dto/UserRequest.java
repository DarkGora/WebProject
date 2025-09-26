package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank
    @Schema(description = "Логин",
            example = "admin",
            defaultValue = "admin")
    String login;
    @NotBlank
    @Schema(description = "Пароль",
            example = "admin",
            defaultValue = "admin")
    String password;
}