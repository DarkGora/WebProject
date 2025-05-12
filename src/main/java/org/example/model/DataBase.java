package org.example.model;

import lombok.extern.slf4j.Slf4j;
import org.example.repository.EmployeeRepository;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;
import org.example.repository.EducationRepository;
import org.example.repository.EmployeeRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
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
        return Optional.ofNullable(employeeMap.get(id));
    }

    @Override
    public Employee save(Employee employee) {
        if (employee.getId() == null) {
            Long newId = nextId.getAndIncrement();
            employee.setId(newId);
            employeeMap.put(newId, employee);
            return employee;
        } else {
            employeeMap.put(employee.getId(), employee);
            return employee;
        }
    }

    @Override
    public void deleteById(Long id) {
        employeeMap.remove(id);
    }

    @Override
    public void delete(Employee entity) {
        if (entity != null && entity.getId() != null) {
            employeeMap.remove(entity.getId());
        }
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        if (ids != null) {
            ids.forEach(employeeMap::remove);
        }
    }

    @Override
    public void deleteAll(Iterable<? extends Employee> entities) {
        if (entities != null) {
            entities.forEach(this::delete);
        }
    }

    @Override
    public void deleteAll() {
        employeeMap.clear();
    }

    @Override
    public boolean existsById(Long id) {
        return employeeMap.containsKey(id);
    }

    @Override
    public List<Employee> findByNameContainingIgnoreCaseOrEmailContainingIgnoreCaseOrPhoneNumberContaining(String name, String email, String phoneNumber) {
        return List.of();
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        return Optional.empty();
    }

    @Override
    public List<Employee> findByNameContaining(String namePart) {
        return filterEmployees(e -> e.getName().contains(namePart));
    }

    @Override
    public List<Employee> findBySkill(Skills skill) {
        return filterEmployees(e -> e.getSkills() != null && e.getSkills().contains(skill));
    }

    @Override
    public List<Employee> findBySkillsContaining(Skills skill) {
        return filterEmployees(e -> e.getSkills() != null && e.getSkills().contains(skill));
    }

    @Override
    public List<Employee> findByEmailContaining(String emailPart) {
        return filterEmployees(e -> e.getEmail().contains(emailPart));
    }

    @Override
    public long count() {
        return employeeMap.size();
    }

    @Override
    public List<Employee> findByNameContainingIgnoreCase(String namePart) {
        String lowerNamePart = namePart.toLowerCase();
        return filterEmployees(e -> e.getName().toLowerCase().contains(lowerNamePart));
    }

    @Override
    public List<Employee> findByEmailContainingIgnoreCase(String emailPart) {
        String lowerEmailPart = emailPart.toLowerCase();
        return filterEmployees(e -> e.getEmail().toLowerCase().contains(lowerEmailPart));
    }

    @Override
    public List<Employee> findByPhoneNumberContaining(String phonePart) {
        return filterEmployees(e -> e.getPhoneNumber().contains(phonePart));
    }

    @Override
    public Page<Employee> findByNameContaining(String namePart, Pageable pageable) {
        List<Employee> filtered = findByNameContaining(namePart);
        return paginate(filtered, pageable);
    }

    @Override
    public List<Employee> findBySchoolAndSkill(String school, Skills skill) {
        return filterEmployees(e ->
                (school == null || e.getSchool().equals(school)) &&
                        (skill == null || (e.getSkills() != null && e.getSkills().contains(skill)))
        );
    }

    @Override
    public Page<Employee> findAll(Pageable pageable) {
        return paginate(employeeMap.values(), pageable);
    }

    @Override
    public List<Employee> findBySchoolAndSkills(String school, Skills skill) {
        return filterEmployees(e ->
                (school == null || e.getSchool().contains(school)) &&
                        (skill == null || (e.getSkills() != null && e.getSkills().contains(skill)))
        );
    }

    @Override
    public List<Employee> findByNameOrEmail(String name, String email) {
        return filterEmployees(e ->
                e.getName().contains(name) || e.getEmail().contains(email)
        );
    }

    @Override
    public <T> List<T> findBy(Class<T> type) {
        return List.of();
    }

    @Override
    public List<String> findAllDistinctSchools() {
        return employeeMap.values().stream()
                .map(Employee::getSchool)
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public int updateEmailById(Long id, String newEmail) {
        Employee employee = employeeMap.get(id);
        if (employee != null) {
            employee.setEmail(newEmail);
            return 1;
        }
        return 0;
    }

    @Override
    public int deleteBySchool(String school) {
        List<Long> idsToRemove = employeeMap.entrySet().stream()
                .filter(entry -> school.equals(entry.getValue().getSchool()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        idsToRemove.forEach(employeeMap::remove);
        return idsToRemove.size();
    }

    @Override
    public List<Employee> findBySkillsIn(List<Skills> skills) {
        if (skills == null || skills.isEmpty()) {
            return findAll();
        }
        return filterEmployees(e ->
                e.getSkills() != null && !Collections.disjoint(e.getSkills(), skills)
        );
    }

    @Override
    public List<Employee> findByCategory(String category) {
        return List.of();
    }

    // Вспомогательные методы
    private List<Employee> filterEmployees(java.util.function.Predicate<Employee> predicate) {
        return employeeMap.values().stream()
                .filter(predicate)
                .collect(Collectors.toList());
    }

    private Page<Employee> paginate(Collection<Employee> employees, Pageable pageable) {
        List<Employee> list = new ArrayList<>(employees);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());

        return new PageImpl<>(
                list.subList(start, end),
                pageable,
                list.size()
        );
    }

    // Остальные методы интерфейса можно оставить пустыми
    @Override
    public <S extends Employee> List<S> saveAll(Iterable<S> entities) {
        List<S> savedEntities = new ArrayList<>();
        if (entities != null) {
            for (S entity : entities) {
                savedEntities.add((S) save(entity));
            }
        }
        return savedEntities;
    }

    @Override
    public List<Employee> findAllById(Iterable<Long> ids) {
        List<Employee> result = new ArrayList<>();
        if (ids != null) {
            for (Long id : ids) {
                Employee employee = employeeMap.get(id);
                if (employee != null) {
                    result.add(employee);
                }
            }
        }
        return result;
    }

    @Override
    public void flush() {

    }

    @Override
    public <S extends Employee> S saveAndFlush(S entity) {
        return (S) save(entity);
    }

    @Override
    public <S extends Employee> List<S> saveAllAndFlush(Iterable<S> entities) {
        return saveAll(entities);
    }

    @Override
    public void deleteAllInBatch(Iterable<Employee> entities) {
        deleteAll(entities);
    }

    @Override
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        deleteAllById(ids);
    }

    @Override
    public void deleteAllInBatch() {
        deleteAll();
    }

    @Override
    public Employee getOne(Long id) {
        return employeeMap.get(id);
    }

    @Override
    public Employee getById(Long id) {
        return employeeMap.get(id);
    }

    @Override
    public Employee getReferenceById(Long id) {
        return employeeMap.get(id);
    }

    @Override
    public <S extends Employee> Optional<S> findOne(Example<S> example) {
        return Optional.empty();
    }

    @Override
    public <S extends Employee> List<S> findAll(Example<S> example) {
        return List.of();
    }

    @Override
    public <S extends Employee> List<S> findAll(Example<S> example, Sort sort) {
        return List.of();
    }

    @Override
    public <S extends Employee> Page<S> findAll(Example<S> example, Pageable pageable) {
        return null;
    }

    @Override
    public <S extends Employee> long count(Example<S> example) {
        return 0;
    }

    @Override
    public <S extends Employee> boolean exists(Example<S> example) {
        return false;
    }

    @Override
    public <S extends Employee, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null;
    }

    @Override
    public List<Employee> findAll(Sort sort) {
        List<Employee> employees = new ArrayList<>(employeeMap.values());
        if (sort != null && !sort.isUnsorted()) {
            employees.sort((e1, e2) -> {
                for (Sort.Order order : sort) {
                    int comparison = compareEmployees(e1, e2, order);
                    if (comparison != 0) {
                        return order.isAscending() ? comparison : -comparison;
                    }
                }
                return 0;
            });
        }
        return employees;
    }
    private int compareEmployees(Employee e1, Employee e2, Sort.Order order) {
        String property = order.getProperty();
        switch (property) {
            case "name":
                return e1.getName().compareTo(e2.getName());
            case "email":
                return e1.getEmail().compareTo(e2.getEmail());
            case "phoneNumber":
                return e1.getPhoneNumber().compareTo(e2.getPhoneNumber());
            default:
                return 0;
        }
    }
    // ... остальные существующие методы без изменений ...
}