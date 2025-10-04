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
    POSTGRESQL("PostgreSQL", "Backend"),
    MYSQL("MySQL", "Backend"),
    MONGODB("MongoDB", "Backend"),
    PYTHON("Python", "Backend"),
    DJANGO("Django", "Backend"),
    FLASK("Flask", "Backend"),
    NODE_JS("Node.js", "Backend"),
    EXPRESS("Express", "Backend"),

    // Frontend
    HTML("HTML", "Frontend"),
    CSS("CSS", "Frontend"),
    JAVASCRIPT("JavaScript", "Frontend"),
    TYPESCRIPT("TypeScript", "Frontend"),
    REACT("React", "Frontend"),
    ANGULAR("Angular", "Frontend"),
    VUE("Vue.js", "Frontend"),
    SVELTE("Svelte", "Frontend"),

    // DevOps
    DOCKER("Docker", "DevOps"),
    KUBERNETES("Kubernetes", "DevOps"),
    AWS("AWS", "DevOps"),
    AZURE("Azure", "DevOps"),
    GCP("Google Cloud", "DevOps"),
    JENKINS("Jenkins", "DevOps"),
    GITLAB_CI("GitLab CI", "DevOps"),
    GITHUB_ACTIONS("GitHub Actions", "DevOps"),

    // Tools
    GIT("Git", "Tools"),
    MAVEN("Maven", "Tools"),
    GRADLE("Gradle", "Tools"),
    INTELLIJ_IDEA("IntelliJ IDEA", "Tools"),
    ECLIPSE("Eclipse", "Tools"),
    VS_CODE("VS Code", "Tools"),

    // Testing
    JUNIT("JUnit", "Testing"),
    MOCKITO("Mockito", "Testing"),
    TESTNG("TestNG", "Testing"),
    SELENIUM("Selenium", "Testing"),
    PYTEST("Pytest", "Testing"), // ДОБАВЬТЕ ЭТО
    JEST("Jest", "Testing"),
    CYPRESS("Cypress", "Testing"),
    POSTMAN("Postman", "Tools"),
    SOAP_UI("SoapUI", "Testing"),

    // Mobile
    ANDROID("Android", "Mobile"),
    KOTLIN("Kotlin", "Mobile"),
    SWIFT("Swift", "Mobile"),
    REACT_NATIVE("React Native", "Mobile"),
    FLUTTER("Flutter", "Mobile"),

    // Data
    PANDAS("Pandas", "Data"),
    NUMPY("NumPy", "Data"),
    TENSORFLOW("TensorFlow", "Data"),
    PYTORCH("PyTorch", "Data"),
    APACHE_SPARK("Apache Spark", "Data"),

    // Other
    LINUX("Linux", "Other"),
    BASH("Bash", "Other"),
    POWERSHELL("PowerShell", "Other");

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

        // Приводим к верхнему регистру и заменяем пробелы и точки
        String normalizedValue = value.toUpperCase()
                .replace(" ", "_")
                .replace(".", "")
                .replace("-", "_")
                .trim();

        try {
            return valueOf(normalizedValue);
        } catch (IllegalArgumentException e) {
            // Дополнительная логика для обработки специальных случаев
            return handleSpecialCases(value, normalizedValue);
        }
    }

    private static Skills handleSpecialCases(String originalValue, String normalizedValue) {
        // Обработка специальных случаев или синонимов
        Map<String, Skills> synonyms = Map.ofEntries(
                // Backend
                Map.entry("POSTGRESQL", POSTGRESQL),
                Map.entry("MYSQL", MYSQL),
                Map.entry("MONGODB", MONGODB),
                Map.entry("PYTHON", PYTHON),
                Map.entry("DJANGO", DJANGO),
                Map.entry("FLASK", FLASK),
                Map.entry("NODE_JS", NODE_JS),
                Map.entry("NODEJS", NODE_JS),
                Map.entry("EXPRESS", EXPRESS),

                // Frontend
                Map.entry("TYPESCRIPT", TYPESCRIPT),
                Map.entry("REACT", REACT),
                Map.entry("ANGULAR", ANGULAR),
                Map.entry("VUE", VUE),
                Map.entry("SVELTE", SVELTE),

                // DevOps
                Map.entry("DOCKER", DOCKER),
                Map.entry("KUBERNETES", KUBERNETES),
                Map.entry("AWS", AWS),
                Map.entry("AZURE", AZURE),
                Map.entry("GCP", GCP),
                Map.entry("JENKINS", JENKINS),
                Map.entry("GITLAB_CI", GITLAB_CI),
                Map.entry("GITHUB_ACTIONS", GITHUB_ACTIONS),

                // Testing
                Map.entry("PYTEST", PYTEST),
                Map.entry("JEST", JEST),
                Map.entry("CYPRESS", CYPRESS),
                Map.entry("POSTMAN", POSTMAN),
                Map.entry("SOAP_UI", SOAP_UI),

                // Mobile
                Map.entry("ANDROID", ANDROID),
                Map.entry("KOTLIN", KOTLIN),
                Map.entry("SWIFT", SWIFT),
                Map.entry("REACT_NATIVE", REACT_NATIVE),
                Map.entry("FLUTTER", FLUTTER),

                // Common synonyms
                Map.entry("TESTING", JUNIT),
                Map.entry("SPRING_FRAMEWORK", SPRING),
                Map.entry("SPRINGBOOT", SPRING_BOOT),
                Map.entry("VUE_JS", VUE),
                Map.entry("VUEJS", VUE),
                Map.entry("JS", JAVASCRIPT),
                Map.entry("TYPESCRIPT", TYPESCRIPT),
                Map.entry("INTELLIJ", INTELLIJ_IDEA),
                Map.entry("VSCode", VS_CODE),
                Map.entry("VSCODE", VS_CODE)
        );

        // Проверяем по normalizedValue
        if (synonyms.containsKey(normalizedValue)) {
            return synonyms.get(normalizedValue);
        }

        // Также проверяем по displayName (без учета регистра)
        for (Skills skill : values()) {
            if (skill.getDisplayName().equalsIgnoreCase(originalValue.trim())) {
                return skill;
            }
        }

        logUnknownSkill(originalValue);
        return null;
    }
    private static void logUnknownSkill(String skillValue) {
        // Логируем неизвестный навык для отладки
        System.err.println("Предупреждение: Неизвестный навык '" + skillValue + "'");
    }
}
