package org.example.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "education")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "year_start", nullable = false)
    private Integer yearStart;

    @Column(name = "year_end", nullable = false)
    private Integer yearEnd;

    @Column(name = "university", nullable = false, length = 100)
    private String university;

    @Column(name = "degree", nullable = false, length = 100)
    private String degree;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;
}
