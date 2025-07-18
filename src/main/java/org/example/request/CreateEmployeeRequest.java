package org.example.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateEmployeeRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String description;
    @NotBlank
    @Size(min = 1, max = 255)
    private String developerName;
}

