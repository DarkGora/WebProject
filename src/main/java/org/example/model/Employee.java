package org.example.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"skills", "educations"})
@EqualsAndHashCode(of = {"id", "email"})
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Override
    public String toString() {
        return "Employee{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", photoPath='" + photoPath + '\'' +
                '}';
    }

    @NotBlank(message = "Имя не может быть пустым")
    @Size(max = 255, message = "Имя не должно превышать 255 символов")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true; // По умолчанию сотрудник активен

    @Size(max = 255, message = "Должность не должна превышать 255 символов")
    @Column(name = "position")
    private String position; // Новое поле для должности

    @Size(max = 255, message = "Отдел не должен превышать 255 символов")
    @Column(name = "department")
    private String department;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Pattern(regexp = "\\+[0-9]{10,15}", message = "Телефон должен начинаться с '+' и содержать от 10 до 15 цифр")
    @Column(name = "phone_number")
    private String phoneNumber;

    @Size(max = 1000, message = "Информация об образовании не должна превышать 1000 символов")
    @Column(name = "school")
    private String school;

    @Size(max = 500, message = "Информация о себе не должна превышать 500 символов")
    @Column(name = "about")
    private String about;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Size(max = 255, message = "Путь к фото не должен превышать 255 символов")
    @Column(name = "photo_path")
    private String photoPath;

    @Size(max = 2000, message = "Резюме не должно превышать 2000 символов")
    @Column(name = "resume")
    private String resume;

    @Size(max = 255, message = "Telegram не должен превышать 255 символов")
    @Column(name = "telegram")
    private String telegram;

    @ElementCollection
    @CollectionTable(name = "employee_skills",
            joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "skill")
    @JsonIgnore
    private Set<String> skills = new HashSet<>();

    @Valid
    @Builder.Default
    @JsonIgnore
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Education> educations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}