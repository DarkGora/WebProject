package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.example.fileFabrica.FileFormat;

@Getter
@Setter
@Data
public class SentFileRequest {
    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    private String email;
    private FileFormat fileFormat;

}
