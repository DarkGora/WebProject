package org.example.model.dto;

import lombok.Data;
import org.example.model.Employee;

import java.time.LocalDateTime;

@Data
public class EmployeeStatusDTO {
    private Long id;
    private String name;
    private String position;
    private String department;
    private boolean deleted;
    private boolean active;
    private LocalDateTime deletedAt;
    private String deletedBy;
    private LocalDateTime lastModified;

    public EmployeeStatusDTO(Employee employee) {
        this.id = employee.getId();
        this.name = employee.getName();
        this.position = employee.getPosition();
        this.department = employee.getDepartment();
        this.deleted = employee.isDeleted();
        this.active = employee.isActive();
        this.deletedAt = employee.getDeletedAt();
        this.deletedBy = employee.getDeletedBy();
        this.lastModified = employee.getUpdatedAt() != null ?
                employee.getUpdatedAt() : employee.getCreatedAt();
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

    public boolean isRestorable() {
        return deleted;
    }
}