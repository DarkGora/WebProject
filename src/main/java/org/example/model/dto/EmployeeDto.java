package org.example.model.dto;


import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
    private String position;

}