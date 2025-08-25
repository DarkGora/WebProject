package org.example.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@NotBlank
@Email
@Setter
@Getter
@Builder
@ToString

public class CreateEmployeeRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String description;
    @NotBlank
    @Size(min = 1, max = 255)
    private String developerName;
}

