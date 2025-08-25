package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
public class EmployeeDto {
    private Long id;
    private String name;
    private String phone;
    private String email;
    private String comment;

}