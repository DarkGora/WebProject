package org.example.model;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Перечисление навыков сотрудников с категоризацией.
 */
public enum Skills {
    // Backend навыки
    JAVA("Backend"),
    SPRING("Backend"),
    SPRING_BOOT("Backend"),
    HIBERNATE("Backend"),
    JPA("Backend"),
    SQL("Backend"),

    // Frontend навыки
    HTML("Frontend"),
    CSS("Frontend"),
    JAVASCRIPT("Frontend"),
    REACT("Frontend"),
    ANGULAR("Frontend"),
    VUE("Frontend"),

    // DevOps навыки
    DOCKER("DevOps"),
    KUBERNETES("DevOps"),
    AWS("DevOps"),
    AZURE("DevOps"),

    // Другие навыки
    GIT("Tools"),
    MAVEN("Tools"),
    GRADLE("Tools"),
    JUNIT("Testing"),
    MOCKITO("Testing"),
    POSTMAN("Tools"),
    TESTING("Testing");

    private final String category;

    Skills(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }


    public static List<Skills> getByCategory(String category) {
        if (category == null || category.isBlank()) {
            return List.of();
        }
        return Arrays.stream(values())
                .filter(skill -> skill.getCategory().equalsIgnoreCase(category))
                .collect(Collectors.toList());
    }


    public static List<String> getAllCategories() {
        return Arrays.stream(values())
                .map(Skills::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }
}