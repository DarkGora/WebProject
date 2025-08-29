package org.example.fileFabrica;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

@Getter
@Setter
@Data
public class SentFileRequest {
    @NotBlank
    @Email
    private String email;
    private FileFormat fileFormat = FileFormat.DOCX;

}
