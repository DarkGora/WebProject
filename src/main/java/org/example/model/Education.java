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

    @Min(value = 1900, message = "Год начала должен быть не ранее 1900")
    @Column(name = "year_start")
    private int yearStart;

    @Min(value = 1900, message = "Год окончания должен быть не ранее 1900")
    @NotNull(message = "Год окончания не может быть null")
    @Column(name = "year_end")
    private Integer yearEnd;

    @NotBlank(message = "Название университета не может быть пустым")
    @Size(max = 100, message = "Название университета не должно превышать 100 символов")
    private String university;

    @NotBlank(message = "Степень не может быть пустой")
    @Size(max = 100, message = "Степень не должна превышать 100 символов")
    private String degree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @AssertTrue(message = "Год окончания должен быть больше или равен году начала")
    private boolean isValidYearRange() {
        return yearEnd != null && yearEnd >= yearStart;
    }
}