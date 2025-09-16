package org.example.model;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Skills {
    // Backend
    JAVA("Java", "Backend"),
    SPRING("Spring Framework", "Backend"),
    SPRING_BOOT("Spring Boot", "Backend"),
    HIBERNATE("Hibernate", "Backend"),
    JPA("JPA", "Backend"),
    SQL("SQL", "Backend"),

    // Frontend
    HTML("HTML", "Frontend"),
    CSS("CSS", "Frontend"),
    JAVASCRIPT("JavaScript", "Frontend"),
    REACT("React", "Frontend"),
    ANGULAR("Angular", "Frontend"),
    VUE("Vue.js", "Frontend"),

    // DevOps
    DOCKER("Docker", "DevOps"),
    KUBERNETES("Kubernetes", "DevOps"),
    AWS("AWS", "DevOps"),
    AZURE("Azure", "DevOps"),

    // Tools
    GIT("Git", "Tools"),
    MAVEN("Maven", "Tools"),
    GRADLE("Gradle", "Tools"),

    // Testing
    JUNIT("JUnit", "Testing"),
    MOCKITO("Mockito", "Testing"),
    POSTMAN("Postman", "Tools"),
    SELENIUM("Selenium", "Testing");

    private final String displayName;
    private final String category;

    Skills(String displayName, String category) {
        this.displayName = displayName;
        this.category = category;
    }
    @JsonValue
    public String getValue() {
        return displayName;

    }

    public String getDisplayName() {
        return displayName;
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

    public static Map<String, List<Skills>> getSkillsGroupedByCategory() {
        return Arrays.stream(values())
                .collect(Collectors.groupingBy(
                        Skills::getCategory,
                        Collectors.toList()
                ));
    }

    public static List<String> getAllCategories() {
        return Arrays.stream(values())
                .map(Skills::getCategory)
                .distinct()
                .collect(Collectors.toList());
    }

    public static Skills fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return valueOf(value.toUpperCase().replace(" ", "_").replace(".", ""));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}