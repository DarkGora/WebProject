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
    PYTEST("Pytest", "Testing"),
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

        String trimmedValue = value.trim();

        System.out.println("Поиск навыка: '" + trimmedValue + "'"); // DEBUG

        // 1. Прямое преобразование (как хранится в БД)
        try {
            Skills directMatch = Skills.valueOf(trimmedValue.toUpperCase());
            System.out.println("Найден прямым преобразованием: " + directMatch); // DEBUG
            return directMatch;
        } catch (IllegalArgumentException e) {
            System.out.println("Прямое преобразование не удалось"); // DEBUG
        }

        // 2. Нормализация строки для поиска по имени enum
        String normalized = normalizeForEnumSearch(trimmedValue);
        try {
            Skills normalizedMatch = Skills.valueOf(normalized);
            System.out.println("Найден после нормализации '" + normalized + "': " + normalizedMatch); // DEBUG
            return normalizedMatch;
        } catch (IllegalArgumentException e) {
            System.out.println("Поиск по нормализованному имени не удалось: " + normalized); // DEBUG
        }

        // 3. Поиск по displayName (без учета регистра)
        for (Skills skill : values()) {
            if (skill.getDisplayName().equalsIgnoreCase(trimmedValue)) {
                System.out.println("Найден по displayName: " + skill); // DEBUG
                return skill;
            }
        }

        // 4. Поиск по частичному совпадению displayName
        for (Skills skill : values()) {
            if (skill.getDisplayName().toLowerCase().contains(trimmedValue.toLowerCase()) ||
                    trimmedValue.toLowerCase().contains(skill.getDisplayName().toLowerCase())) {
                System.out.println("Найден по частичному совпадению: " + skill); // DEBUG
                return skill;
            }
        }

        // 5. Специальные случаи для удобства
        Map<String, Skills> specialCases = Map.ofEntries(
                Map.entry("spring", SPRING),
                Map.entry("spring boot", SPRING_BOOT),
                Map.entry("springboot", SPRING_BOOT),
                Map.entry("node.js", NODE_JS),
                Map.entry("nodejs", NODE_JS),
                Map.entry("vue.js", VUE),
                Map.entry("vuejs", VUE),
                Map.entry("vs code", VS_CODE),
                Map.entry("vscode", VS_CODE),
                Map.entry("gitlab ci", GITLAB_CI),
                Map.entry("gitlabci", GITLAB_CI),
                Map.entry("github actions", GITHUB_ACTIONS),
                Map.entry("githubactions", GITHUB_ACTIONS),
                Map.entry("react native", REACT_NATIVE),
                Map.entry("reactnative", REACT_NATIVE),
                Map.entry("apache spark", APACHE_SPARK),
                Map.entry("apachespark", APACHE_SPARK),
                Map.entry("google cloud", GCP),
                Map.entry("googlecloud", GCP),
                Map.entry("postgresql", POSTGRESQL),
                Map.entry("postgres", POSTGRESQL),
                Map.entry("mysql", MYSQL),
                Map.entry("mongodb", MONGODB),
                Map.entry("intellij idea", INTELLIJ_IDEA),
                Map.entry("intellij", INTELLIJ_IDEA),
                Map.entry("pytest", PYTEST),
                Map.entry("soapui", SOAP_UI),
                Map.entry("soap ui", SOAP_UI)
        );

        String lowerValue = trimmedValue.toLowerCase();
        Skills specialCase = specialCases.get(lowerValue);
        if (specialCase != null) {
            System.out.println("Найден в specialCases: " + specialCase); // DEBUG
            return specialCase;
        }

        System.out.println("Навык не найден: '" + trimmedValue + "'"); // DEBUG
        return null;
    }

    private static String normalizeForEnumSearch(String value) {
        return value.toUpperCase()
                .replace(" ", "_")
                .replace(".", "")
                .replace("-", "_")
                .replace("__", "_") // Убираем двойные подчеркивания
                .trim();
    }

    // Метод для отладки - показывает все доступные навыки
    public static void printAllSkills() {
        System.out.println("=== ДОСТУПНЫЕ НАВЫКИ ===");
        for (Skills skill : values()) {
            System.out.println(skill.name() + " -> \"" + skill.getDisplayName() + "\" (" + skill.getCategory() + ")");
        }
        System.out.println("========================");
    }

    // Метод для поиска по любому возможному варианту (самый агрессивный поиск)
    public static Skills findAny(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim().toLowerCase();

        // Пробуем все возможные варианты
        for (Skills skill : values()) {
            // Сравниваем с name (верхний регистр)
            if (skill.name().toLowerCase().equals(trimmed)) {
                return skill;
            }
            // Сравниваем с displayName
            if (skill.getDisplayName().toLowerCase().equals(trimmed)) {
                return skill;
            }
            // Сравниваем с нормализованным name
            if (normalizeForEnumSearch(skill.name()).toLowerCase().equals(trimmed)) {
                return skill;
            }
        }

        return fromString(value);
    }
}