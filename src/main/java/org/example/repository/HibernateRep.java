package org.example.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Employee;
import org.example.model.Skills;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Repository
@RequiredArgsConstructor
@Transactional
public class HibernateRep implements EmployeeRepository {
    private final SessionFactory sessionFactory;

    private Session getCurrentSession() {
        try {
            Session session = sessionFactory.getCurrentSession();
            if (session == null) {
                throw new IllegalStateException("No active session found");
            }

            // Явная проверка и начало транзакции, если необходимо
            if (!session.getTransaction().isActive()) {
                log.warn("Транзакция не активна, начинаем новую");
                session.beginTransaction();
            }

            return session;
        } catch (Exception e) {
            log.error("Ошибка при получении сессии", e);
            throw new IllegalStateException("Failed to get session", e);
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findAll() {
        log.debug("Получение всех сотрудников");
        return getCurrentSession()
                .createQuery("FROM Employee", Employee.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findAllSorted(String sortField, boolean ascending) {
        if (sortField == null || sortField.isBlank()) {
            return findAll();
        }
        String direction = ascending ? "ASC" : "DESC";
        String hql = switch (sortField.toLowerCase()) {
            case "name" -> "FROM Employee e ORDER BY e.name " + direction;
            case "email" -> "FROM Employee e ORDER BY e.email " + direction;
            case "phonenumber" -> "FROM Employee e ORDER BY e.phoneNumber " + direction;
            case "school" -> "FROM Employee e ORDER BY e.school " + direction;
            case "createdat" -> "FROM Employee e ORDER BY e.createdAt " + direction;
            default -> {
                log.warn("Неподдерживаемое поле сортировки: {}, возвращаются все сотрудники", sortField);
                yield "FROM Employee";
            }
        };
        log.debug("Получение сотрудников с сортировкой: {} {}", sortField, direction);
        return getCurrentSession()
                .createQuery(hql, Employee.class)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findAllPaginated(int offset, int limit) {
        log.debug("Получение сотрудников с пагинацией: offset={}, limit={}", offset, limit);
        try {
            return getCurrentSession()
                    .createQuery("FROM Employee", Employee.class)
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при выполнении запроса пагинации: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public long count() {
        log.debug("Подсчёт общего количества сотрудников");
        try {
            return getCurrentSession()
                    .createQuery("SELECT COUNT(*) FROM Employee", Long.class)
                    .getSingleResult();
        } catch (Exception e) {
            log.error("Ошибка при подсчёте сотрудников: {}", e.getMessage(), e);
            throw e;
        }
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public Optional<Employee> findById(Long id) {
        if (id == null) {
            log.warn("Попытка найти сотрудника с null ID");
            return Optional.empty();
        }
        log.debug("Получение сотрудника по ID: {}", id);
        return Optional.ofNullable(getCurrentSession().get(Employee.class, id));
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public Optional<Employee> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Попытка найти сотрудника с null или пустым email");
            return Optional.empty();
        }
        log.debug("Получение сотрудника по email: {}", email);
        return Optional.ofNullable(getCurrentSession()
                .createQuery("FROM Employee e WHERE e.email = :email", Employee.class)
                .setParameter("email", email)
                .uniqueResult());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Employee save(Employee employee) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        if (employee.getId() == null && findByEmail(employee.getEmail()).isPresent()) {
            log.error("Email {} уже существует", employee.getEmail());
            throw new IllegalArgumentException("Email " + employee.getEmail() + " уже существует");
        }
        if (employee.getId() == null) {
            log.info("Сохранение нового сотрудника: {}", employee.getName());
            getCurrentSession().persist(employee);
        } else {
            log.info("Обновление существующего сотрудника: {} (ID: {})", employee.getName(), employee.getId());
            getCurrentSession().merge(employee);
        }
        return employee;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public <S extends Employee> List<S> saveAll(Iterable<S> employees) {
        Objects.requireNonNull(employees, "Сущности не могут быть null");
        List<S> result = new ArrayList<>();
        for (S entity : employees) {
            result.add((S) save(entity));
        }
        log.info("Сохранено {} сотрудников", result.size());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteById(Long id) {
        if (id == null) {
            log.warn("Попытка удалить сотрудника с null ID");
            return;
        }
        Employee employee = getCurrentSession().get(Employee.class, id);
        if (employee != null) {
            log.info("Удаление сотрудника с ID: {}", id);
            getCurrentSession().remove(employee);
        } else {
            log.warn("Сотрудник с ID {} не найден для удаления", id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void delete(Employee employee) {
        if (employee == null || employee.getId() == null) {
            log.warn("Попытка удалить null или недействительного сотрудника");
            return;
        }
        log.info("Удаление сотрудника: {} (ID: {})", employee.getName(), employee.getId());
        if (getCurrentSession().contains(employee)) {
            getCurrentSession().remove(employee);
        } else {
            getCurrentSession().remove(getCurrentSession().merge(employee));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteAllById(Iterable<? extends Long> ids) {
        if (ids == null) {
            log.warn("Попытка удалить сотрудников с null ID");
            return;
        }
        List<Long> idList = StreamSupport.stream(ids.spliterator(), false)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!idList.isEmpty()) {
            log.info("Удаление сотрудников с ID: {}", idList);
            getCurrentSession()
                    .createMutationQuery("DELETE FROM Employee e WHERE e.id IN :ids")
                    .setParameter("ids", idList)
                    .executeUpdate();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteAll(Iterable<? extends Employee> employees) {
        if (employees == null) {
            log.warn("Попытка удалить null сущности");
            return;
        }
        List<Long> ids = StreamSupport.stream(employees.spliterator(), false)
                .filter(Objects::nonNull)
                .map(Employee::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            log.info("Удаление {} сотрудников", ids.size());
            deleteAllById(ids);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void deleteAll() {
        log.info("Удаление всех сотрудников");
        getCurrentSession()
                .createMutationQuery("DELETE FROM Employee")
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }
        log.debug("Проверка существования сотрудника с ID: {}", id);
        return getCurrentSession()
                .createQuery("SELECT COUNT(e) > 0 FROM Employee e WHERE e.id = :id", Boolean.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findByNameContaining(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            log.debug("Часть имени null или пустая, возвращаются все сотрудники");
            return findAll();
        }
        log.debug("Получение сотрудников с именем, содержащим: {}", namePart);
        return getCurrentSession()
                .createQuery("SELECT e FROM Employee e WHERE LOWER(e.name) LIKE LOWER(:namePart)", Employee.class)
                .setParameter("namePart", "%" + namePart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findByNameContainingPaginated(String namePart, int offset, int limit) {
        if (namePart == null || namePart.isBlank()) {
            log.debug("Часть имени null или пустая, возвращаются все сотрудники с пагинацией");
            return findAllPaginated(offset, limit);
        }
        log.debug("Получение сотрудников с именем, содержащим: {} с пагинацией", namePart);
        return getCurrentSession()
                .createQuery("SELECT e FROM Employee e WHERE LOWER(e.name) LIKE LOWER(:namePart)", Employee.class)
                .setParameter("namePart", "%" + namePart + "%")
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findBySkill(Skills skill) {
        if (skill == null) {
            log.debug("Навык null, возвращаются все сотрудники");
            return findAll();
        }
        log.debug("Получение сотрудников с навыком: {}", skill);
        return getCurrentSession()
                .createQuery("SELECT e FROM Employee e WHERE :skill MEMBER OF e.skills", Employee.class)
                .setParameter("skill", skill)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findBySkills(List<Skills> skills) {
        if (skills == null || skills.isEmpty()) {
            log.debug("Список навыков null или пуст, возвращаются все сотрудники");
            return findAll();
        }
        log.debug("Получение сотрудников с навыками: {}", skills);
        return getCurrentSession()
                .createQuery("SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE s IN :skills", Employee.class)
                .setParameter("skills", skills)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findBySkillCategory(String category) {
        if (category == null || category.isBlank()) {
            log.debug("Категория null или пустая, возвращаются все сотрудники");
            return findAll();
        }
        log.debug("Получение сотрудников с категорией: {}", category);
        return getCurrentSession()
                .createQuery("SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE s.category = :category", Employee.class)
                .setParameter("category", category)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findByEmailContaining(String emailPart) {
        if (emailPart == null || emailPart.isBlank()) {
            log.debug("Часть email null или пустая, возвращаются все сотрудники");
            return findAll();
        }
        log.debug("Получение сотрудников с email, содержащим: {}", emailPart);
        return getCurrentSession()
                .createQuery("SELECT e FROM Employee e WHERE LOWER(e.email) LIKE LOWER(:emailPart)", Employee.class)
                .setParameter("emailPart", "%" + emailPart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findByPhoneNumberContaining(String phonePart) {
        if (phonePart == null || phonePart.isBlank()) {
            log.debug("Часть номера телефона null или пустая, возвращаются все сотрудники");
            return findAll();
        }
        log.debug("Получение сотрудников с номером телефона, содержащим: {}", phonePart);
        return getCurrentSession()
                .createQuery("SELECT e FROM Employee e WHERE e.phoneNumber LIKE :phonePart", Employee.class)
                .setParameter("phonePart", "%" + phonePart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findBySchoolAndSkill(String school, Skills skill) {
        if ((school == null || school.isBlank()) && skill == null) {
            log.debug("Школа и навык null или пустые, возвращаются все сотрудники");
            return findAll();
        }
        String query = "SELECT e FROM Employee e WHERE " +
                (school != null && !school.isBlank() ? "e.school LIKE :school" : "1=1") +
                (skill != null ? " AND :skill MEMBER OF e.skills" : "");
        log.debug("Получение сотрудников с школой: {} и навыком: {}", school, skill);
        var hqlQuery = getCurrentSession().createQuery(query, Employee.class);
        if (school != null && !school.isBlank()) {
            hqlQuery.setParameter("school", "%" + school + "%");
        }
        if (skill != null) {
            hqlQuery.setParameter("skill", skill);
        }
        return hqlQuery.getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<Employee> findByNameOrEmail(String name, String email) {
        if ((name == null || name.isBlank()) && (email == null || email.isBlank())) {
            log.debug("Имя и email null или пустые, возвращаются все сотрудники");
            return findAll();
        }
        log.debug("Получение сотрудников с именем: {} или email: {}", name, email);
        return getCurrentSession()
                .createQuery("SELECT e FROM Employee e WHERE " +
                        "(:name IS NULL OR LOWER(e.name) LIKE LOWER(:name)) OR " +
                        "(:email IS NULL OR LOWER(e.email) LIKE LOWER(:email))", Employee.class)
                .setParameter("name", name != null ? "%" + name + "%" : null)
                .setParameter("email", email != null ? "%" + email + "%" : null)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true, propagation = Propagation.REQUIRED)
    public List<String> findAllDistinctSchools() {
        log.debug("Получение всех уникальных школ");
        return getCurrentSession()
                .createQuery("SELECT DISTINCT e.school FROM Employee e WHERE e.school IS NOT NULL", String.class)
                .getResultList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public int updateEmailById(Long id, String newEmail) {
        if (id == null || newEmail == null || newEmail.isBlank()) {
            log.warn("Недействительные параметры для updateEmailById: id={}, newEmail={}", id, newEmail);
            return 0;
        }
        log.info("Обновление email для сотрудника ID: {} на {}", id, newEmail);
        return getCurrentSession()
                .createMutationQuery("UPDATE Employee e SET e.email = :email WHERE e.id = :id")
                .setParameter("email", newEmail)
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public int deleteBySchool(String school) {
        if (school == null || school.isBlank()) {
            log.warn("Попытка удалить сотрудников с null или пустой школой");
            return 0;
        }
        log.info("Удаление сотрудников с школой: {}", school);
        return getCurrentSession()
                .createMutationQuery("DELETE FROM Employee e WHERE e.school = :school")
                .setParameter("school", school)
                .executeUpdate();
    }
}