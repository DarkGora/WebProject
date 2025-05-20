package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "educations")
public class Education {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Год начала не может быть null")
    @Min(value = 1900, message = "Год начала должен быть не ранее 1900")
    @Column(name = "year_start")
    private Integer yearStart;

    @NotNull(message = "Год окончания не может быть null")
    @Min(value = 1900, message = "Год окончания должен быть не ранее 1900")
    @Column(name = "year_end")
    private Integer yearEnd;

    @NotBlank(message = "Название университета не может быть пустым")
    @Size(max = 100, message = "Название университета не должно превышать 100 символов")
    private String university;

    @NotBlank(message = "Степень не может быть пустой")
    @Size(max = 100, message = "Степень не должна превышать 100 символов")
    private String degree;

    @NotNull(message = "Сотрудник не может быть null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @AssertTrue(message = "Год окончания должен быть больше или равен году начала")
    private boolean isValidYearRange() {
        return yearEnd != null && yearStart != null && yearEnd >= yearStart;
    }
}