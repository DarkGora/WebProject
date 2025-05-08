package org.example.model;

import lombok.extern.slf4j.Slf4j;
import org.example.EmployeeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class DataBase implements EmployeeRepository {
    private final Map<Long, Employee> employeeMap = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1L);

    public DataBase() {
        initializeTestData();
        log.info("In-memory database initialized with {} test employees", employeeMap.size());
    }

    private void initializeTestData() {
        List<Employee> testEmployees = List.of(
                Employee.builder()
                        .name("Евушко Андрей")
                        .phoneNumber("+375336980732")
                        .email("andrey@example.com")
                        .telegram("@ansgoo")
                        .resume("Опыт работы: 2 года Java разработчиком")
                        .school("БГУИР")
                        .photoPath("/images/employee1.jpg")
                        .skill("Backend разработка на Java")
                        .skills(new ArrayList<>(List.of(Skills.JAVA, Skills.SPRING, Skills.SQL)))
                        .build(),

                Employee.builder()
                        .name("Вася Пупкин")
                        .phoneNumber("+375291234567")
                        .email("vasya@example.com")
                        .telegram("@vasya")
                        .resume("Опыт работы: 5 лет Full-stack разработчиком")
                        .school("БГУ")
                        .photoPath("/images/employee2.jpg")
                        .skill("Full-stack разработка")
                        .skills(new ArrayList<>(List.of(Skills.JAVA, Skills.SPRING_BOOT, Skills.REACT)))
                        .build()
        );

        testEmployees.forEach(this::save);
    }

    @Override
    public List<Employee> findAll() {
        return new ArrayList<>(employeeMap.values());
    }

    @Override
    public Optional<Employee> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        Employee employee = employeeMap.get(id);
        return Optional.ofNullable(employee != null ? copyEmployee(employee) : null);
    }

    @Override
    public Employee save(Employee employee) {
        Objects.requireNonNull(employee, "Employee cannot be null");

        if (employee.getId() == null) {
            Long newId = nextId.getAndIncrement();
            Employee newEmployee = copyEmployee(employee);
            newEmployee.setId(newId);
            employeeMap.put(newId, newEmployee);
            log.info("Created new employee: {} (ID: {})", newEmployee.getName(), newId);
            return copyEmployee(newEmployee);
        } else {
            return employeeMap.compute(employee.getId(), (id, existing) -> {
                if (existing == null) {
                    return copyEmployee(employee);
                } else {
                    updateEmployeeFields(existing, employee);
                    return existing;
                }
            });
        }
    }

    @Override
    public void deleteById(Long id) {
        if (id != null && employeeMap.remove(id) != null) {
            log.info("Deleted employee with ID: {}", id);
        }
    }

    @Override
    public void delete(Employee employee) {
        if (employee != null && employee.getId() != null) {
            deleteById(employee.getId());
        }
    }

    @Override
    public boolean existsById(Long id) {
        return id != null && employeeMap.containsKey(id);
    }

    @Override
    public List<Employee> findByNameContaining(String namePart) {
        return findByNameContainingIgnoreCase(namePart);
    }

    @Override
    public List<Employee> findBySkill(Skills skill) {
        return findBySkillsContaining(skill);
    }

    @Override
    public List<Employee> findBySkillsContaining(Skills skill) {
        if (skill == null) {
            return findAll();
        }

        return employeeMap.values().stream()
                .filter(e -> e.getSkills() != null && e.getSkills().contains(skill))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public List<Employee> findByEmailContaining(String emailPart) {
        return findByEmailContainingIgnoreCase(emailPart);
    }

    @Override
    public long count() {
        return employeeMap.size();
    }

    @Override
    public List<Employee> findByNameContainingIgnoreCase(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            return findAll();
        }

        String lowerNamePart = namePart.toLowerCase();
        return employeeMap.values().stream()
                .filter(e -> e.getName().toLowerCase().contains(lowerNamePart))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public List<Employee> findByEmailContainingIgnoreCase(String emailPart) {
        if (emailPart == null || emailPart.isBlank()) {
            return findAll();
        }

        String lowerEmailPart = emailPart.toLowerCase();
        return employeeMap.values().stream()
                .filter(e -> e.getEmail().toLowerCase().contains(lowerEmailPart))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public List<Employee> findByPhoneNumberContaining(String phonePart) {
        if (phonePart == null || phonePart.isBlank()) {
            return findAll();
        }

        return employeeMap.values().stream()
                .filter(e -> e.getPhoneNumber() != null && e.getPhoneNumber().contains(phonePart))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public Page<Employee> findAll(Pageable pageable) {
        List<Employee> allEmployees = findAll();
        return getPage(allEmployees, pageable);
    }

    @Override
    public Page<Employee> findByNameContaining(String namePart, Pageable pageable) {
        List<Employee> filtered = findByNameContaining(namePart);
        return getPage(filtered, pageable);
    }

    @Override
    public List<Employee> findBySchoolAndSkills(String school, Skills skill) {
        if ((school == null || school.isBlank()) && skill == null) {
            return findAll();
        }

        return employeeMap.values().stream()
                .filter(e -> (school == null || school.isBlank() ||
                        (e.getSchool() != null && e.getSchool().contains(school))) &&
                        (skill == null ||
                                (e.getSkills() != null && e.getSkills().contains(skill))))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public List<Employee> findByNameOrEmail(String name, String email) {
        if ((name == null || name.isBlank()) && (email == null || email.isBlank())) {
            return findAll();
        }

        return employeeMap.values().stream()
                .filter(e -> (name != null && !name.isBlank() &&
                        e.getName().toLowerCase().contains(name.toLowerCase())) ||
                        (email != null && !email.isBlank() &&
                                e.getEmail().toLowerCase().contains(email.toLowerCase())))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public <T> List<T> findBy(Class<T> type) {
        if (type == String.class) {
            return (List<T>) employeeMap.values().stream()
                    .map(Employee::getName)
                    .collect(Collectors.toList());
        }
        throw new UnsupportedOperationException("Unsupported projection type: " + type.getName());
    }

    @Override
    public List<String> findAllDistinctSchools() {
        return employeeMap.values().stream()
                .map(Employee::getSchool)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public int updateEmailById(Long id, String newEmail) {
        if (id == null || newEmail == null || newEmail.isBlank()) {
            return 0;
        }

        Employee employee = employeeMap.get(id);
        if (employee != null) {
            employee.setEmail(newEmail);
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteBySchool(String school) {
        if (school == null || school.isBlank()) {
            return 0;
        }

        List<Long> idsToRemove = employeeMap.values().stream()
                .filter(e -> school.equals(e.getSchool()))
                .map(Employee::getId)
                .collect(Collectors.toList());

        idsToRemove.forEach(employeeMap::remove);
        return idsToRemove.size();
    }

    private void updateEmployeeFields(Employee existing, Employee newData) {
        existing.setName(newData.getName());
        existing.setPhoneNumber(newData.getPhoneNumber());
        existing.setEmail(newData.getEmail());
        existing.setTelegram(newData.getTelegram());
        existing.setResume(newData.getResume());
        existing.setSchool(newData.getSchool());
        existing.setPhotoPath(newData.getPhotoPath());
        existing.setSkill(newData.getSkill());
        existing.setSkills(newData.getSkills() != null ?
                new ArrayList<>(newData.getSkills()) : new ArrayList<>());
    }

    private Employee copyEmployee(Employee original) {
        return Employee.builder()
                .id(original.getId())
                .name(original.getName())
                .phoneNumber(original.getPhoneNumber())
                .email(original.getEmail())
                .telegram(original.getTelegram())
                .resume(original.getResume())
                .school(original.getSchool())
                .photoPath(original.getPhotoPath())
                .skill(original.getSkill())
                .skills(original.getSkills() != null ?
                        new ArrayList<>(original.getSkills()) : null)
                .build();
    }

    private Page<Employee> getPage(List<Employee> employees, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), employees.size());

        if (start > employees.size()) {
            return new PageImpl<>(Collections.emptyList(), pageable, employees.size());
        }

        return new PageImpl<>(employees.subList(start, end), pageable, employees.size());
    }

    @Override
    public List<Employee> findBySkillsIn(List<Skills> skills) {
        if (skills == null || skills.isEmpty()) {
            return findAll();
        }

        return employeeMap.values().stream()
                .filter(e -> e.getSkills() != null && !Collections.disjoint(e.getSkills(), skills))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }

    @Override
    public List<Employee> findByCategory(String category) {
        if (category == null || category.isBlank()) {
            return findAll();
        }

        return employeeMap.values().stream()
                .filter(e -> e.getSkills() != null &&
                        e.getSkills().stream()
                                .anyMatch(skill -> skill.getCategory().equalsIgnoreCase(category)))
                .map(this::copyEmployee)
                .collect(Collectors.toList());
    }
}