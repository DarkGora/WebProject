package org.example.dto;

import lombok.Data;

import java.util.List;

@Data
public class EmployeeQuickViewDTO {
    private Long id;
    private String name;
    private String position;
    private String department;
    private String email;
    private String phone;
    private boolean active;
    private java.time.LocalDateTime createdAt;
    private String photoPath;
    private List<String> skills;  // ИСПРАВЛЕНИЕ: List<String> вместо Set<String> (для JSON-массива)

    // Геттеры и сеттеры
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhoneNumber() { return phone; }
    public void setPhoneNumber(String phoneNumber) { this.phone = phoneNumber; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public java.time.LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getPhotoPath() { return photoPath; }
    public void setPhotoPath(String photoPath) { this.photoPath = photoPath; }

    public List<String> getSkills() { return skills; }
    public void setSkills(List<String> skills) { this.skills = skills; }  // ИСПРАВЛЕНИЕ: List<String> вместо Set<String>
}