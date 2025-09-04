package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "educations")
public class Education {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Год начала не может быть пустым")
    @Min(value = 1900, message = "Год начала должен быть не ранее 1900")
    @Column(name = "year_start", nullable = false)
    private Integer yearStart;

    @NotNull(message = "Год окончания не может быть пустым")
    @Min(value = 1900, message = "Год окончания должен быть не ранее 1900")
    @Column(name = "year_end", nullable = false)
    private Integer yearEnd;

    @NotBlank(message = "Университет не может быть пустым")
    @Size(max = 100, message = "Название университета не должно превышать 100 символов")
    @Column(name = "university", nullable = false)
    private String university;

    @NotBlank(message = "Степень не может быть пустой")
    @Size(max = 100, message = "Степень не должна превышать 100 символов")
    @Column(name = "degree", nullable = false)
    private String degree;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;


    @AssertTrue(message = "Год окончания должен быть больше или равен году начала")
    public boolean isYearsValid() {
        if (yearStart == null || yearEnd == null) {
            return true;
        }
        return yearEnd >= yearStart;
    }
}