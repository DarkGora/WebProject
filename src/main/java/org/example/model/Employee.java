package org.example.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employee", schema = "public")
@Builder(toBuilder = true)
@ToString(exclude = "photoPath")
public class Employee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "phonenumber", length = 20)
    private String phoneNumber;

    @Column(unique = true, nullable = false, length = 100)
    private String email;

    @Column(length = 50)
    private String telegram;

    @Column(columnDefinition = "text") // или "varchar"
    private String resume;

    @Column(length = 100)
    private String school;

    @Column(name = "photopath")
    private String photoPath;


    @Lob
    private String skill;

    @ElementCollection
    @CollectionTable(
            name = "employee_skills",
            joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "skill")
    @Enumerated(EnumType.STRING)
    private List<Skills> skills = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return Objects.equals(id, employee.id) &&
                Objects.equals(email, employee.email);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, email);
    }

    // Методы для работы с навыками
    public void addSkill(Skills skill) {
        if (skill != null && !skills.contains(skill)) {
            skills.add(skill);
        }
    }

    public void removeSkill(Skills skill) {
        if (skill != null) {
            skills.remove(skill);
        }
    }

    public boolean hasSkill(Skills skill) {
        return skills.contains(skill);
    }
}