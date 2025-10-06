package org.example.model;

import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"skills", "educations", "reviews"})
@EqualsAndHashCode(of = {"id", "email"})
@Entity
@Table(name = "employees")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Имя не может быть пустым")
    @Size(max = 255, message = "Имя не должно превышать 255 символов")
    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Size(max = 255, message = "Должность не должна превышать 255 символов")
    @Column(name = "position")
    private String position;

    @Size(max = 255, message = "Отдел не должен превышать 255 символов")
    @Column(name = "department")
    private String department;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат email")
    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Pattern(regexp = "^\\+[0-9]{10,15}$", message = "Телефон должен начинаться с '+' и содержать от 10 до 15 цифр")
    @Column(name = "phone_number")
    private String phoneNumber;

    @Size(max = 1000, message = "Информация об образовании не должна превышать 1000 символов")
    @Column(name = "school")
    private String school;

    @Size(max = 500, message = "Информация о себе не должна превышать 500 символов")
    @Column(name = "about")
    private String about;

    // === SOFT DELETE ФУНКЦИОНАЛЬНОСТЬ ===
    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy; // Кто удалил (можно хранить username)

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Size(max = 255, message = "Путь к фото не должен превышать 255 символов")
    @Column(name = "photo_path")
    private String photoPath;

    @Size(max = 2000, message = "Резюме не должно превышать 2000 символов")
    @Column(name = "resume")
    private String resume;

    @Pattern(regexp = "^@[a-zA-Z0-9_]{5,32}$", message = "Telegram должен начинаться с @ и содержать от 5 до 32 символов (буквы, цифры, подчеркивания)")
    @Size(max = 255, message = "Telegram не должен превышать 255 символов")
    @Column(name = "telegram")
    private String telegram;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_skills",
            joinColumns = @JoinColumn(name = "employee_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "skill")
    @Builder.Default
    private Set<Skills> skills = new HashSet<>();

    @Valid
    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Education> educations = new ArrayList<>();

    @Valid
    @Builder.Default
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    @Column(name = "email_notifications")
    @Builder.Default
    private boolean emailNotifications = true;

    @Column(name = "sms_notifications")
    @Builder.Default
    private boolean smsNotifications = false;

    @Column(name = "theme")
    @Builder.Default
    private String theme = "dark";

    @Column(name = "language")
    @Builder.Default
    private String language = "ru";

    @Column(name = "items_per_page")
    @Builder.Default
    private int itemsPerPage = 10;

    // === МЕТОДЫ ЖИЗНЕННОГО ЦИКЛА ===

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // === МЕТОДЫ ДЛЯ SOFT DELETE ===


    public void softDelete() {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.active = false; // Деактивируем при удалении
    }

    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedAt = LocalDateTime.now();
        this.deletedBy = deletedBy;
        this.active = false;
    }


    public void restore() {
        this.deleted = false;
        this.deletedAt = null;
        this.deletedBy = null;
        this.active = true; // Активируем при восстановлении
    }

    public boolean isDeleted() {
        return deleted;
    }


    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public String getDeletedBy() {
        return deletedBy;
    }

    // === БИЗНЕС-МЕТОДЫ ===

    public boolean isActiveEmployee() {
        return !deleted && active;
    }

    public String getStatus() {
        if (deleted) {
            return "Удален";
        } else if (!active) {
            return "Неактивен";
        } else {
            return "Активен";
        }
    }

    public LocalDateTime getLastModified() {
        return updatedAt != null ? updatedAt : createdAt;
    }

    public void addSkill(Skills skill) {
        if (this.skills == null) {
            this.skills = new HashSet<>();
        }
        this.skills.add(skill);
    }

    public void removeSkill(Skills skill) {
        if (this.skills != null) {
            this.skills.remove(skill);
        }
    }

    public void addEducation(Education education) {
        if (this.educations == null) {
            this.educations = new ArrayList<>();
        }
        education.setEmployee(this);
        this.educations.add(education);
    }

    public void removeEducation(Education education) {
        if (this.educations != null) {
            this.educations.remove(education);
            education.setEmployee(null);
        }
    }

    public void addReview(Review review) {
        if (this.reviews == null) {
            this.reviews = new ArrayList<>();
        }
        review.setEmployee(this);
        this.reviews.add(review);
    }

    // === ВАЛИДАЦИОННЫЕ МЕТОДЫ ===

    public boolean isEditable() {
        return !deleted;
    }

    public boolean isDeletable() {
        return !deleted;
    }

    public boolean isRestorable() {
        return deleted;
    }

    // === МЕТОДЫ ДЛЯ ОТОБРАЖЕНИЯ ===

    public String getDisplayPhotoPath() {
        return (photoPath != null && !photoPath.trim().isEmpty()) ?
                photoPath : "/images/default.jpg";
    }

    public String getShortInfo() {
        return String.format("%s - %s (%s)", name, position, department);
    }

    public String getSkillsAsString() {
        if (skills == null || skills.isEmpty()) {
            return "Нет навыков";
        }
        return skills.stream()
                .map(Skills::getDisplayName)
                .collect(Collectors.joining(", "));
    }
}