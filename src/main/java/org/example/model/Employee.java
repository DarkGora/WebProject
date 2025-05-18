package org.example.model;

import lombok.*;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "school")
    private String school;

    @Transient // Игнорировать поле в базе данных
    private String about;

    @Transient // Игнорировать поле в базе данных
    private String category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "photo_path")
    private String photoPath;

    @Column(name = "resume")
    private String resume;

    @Column(name = "telegram")
    private String telegram;

    @ElementCollection
    @CollectionTable(name = "employee_skills", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "skill")
    private Set<String> skills = new HashSet<>(); // Используйте Set вместо List

    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Education> educations = new ArrayList<>();

}