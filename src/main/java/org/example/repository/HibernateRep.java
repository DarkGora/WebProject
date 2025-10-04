/*package org.example.repository;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Education;
import org.example.model.Employee;
import org.example.model.Review;
import org.example.model.Skills;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Репозиторий для управления сущностью Employee с использованием Hibernate.
 */
/*@Slf4j
@Repository
public class HibernateRep implements EmployeeRepository, AutoCloseable {
    private final SessionFactory sessionFactory;

    public HibernateRep() {
        try {
            Configuration configuration = new Configuration()
                    .configure()
                    .addPackage("org.example.model")
                    .addAnnotatedClass(Employee.class)
                    .addAnnotatedClass(Review.class)
                    .addAnnotatedClass(Education.class);

            this.sessionFactory = configuration.buildSessionFactory();
            log.info("SessionFactory успешно создан");
        } catch (Exception e) {
            log.error("Ошибка при создании SessionFactory", e);
            throw new RuntimeException("Не удалось инициализировать Hibernate", e);
        }
    }

    /**
     * Получение сессии Hibernate.
     */
   /* private Session getSession() {
        try {
            return sessionFactory.openSession();
        } catch (Exception e) {
            log.error("Ошибка при получении сессии Hibernate", e);
            throw new IllegalStateException("Не удалось получить сессию", e);
        }
    }

    /**
     * Безопасное выполнение операции в транзакции.
     */
   /* private <T> T executeInTransaction(TransactionOperation<T> operation) {
        Session session = getSession();
        try {
            session.beginTransaction();
            T result = operation.execute(session);
            session.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана");
            }
            throw new RuntimeException("Ошибка при выполнении операции: " + e.getMessage(), e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    /**
     * Безопасное выполнение операции без транзакции.
     */
 /*   private <T> T executeWithoutTransaction(QueryOperation<T> operation) {
        Session session = getSession();
        try {
            return operation.execute(session);
        } catch (Exception e) {
            log.error("Ошибка при выполнении запроса", e);
            throw new RuntimeException("Ошибка при выполнении запроса: " + e.getMessage(), e);
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    @Override
    public List<Employee> findAll() {
        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e LEFT JOIN FETCH e.skills", Employee.class)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findAllSorted(String sortField, boolean ascending) {
        if (sortField == null || sortField.isBlank()) {
            return findAll();
        }

        List<String> validFields = List.of("name", "email", "phonenumber", "school", "createdat");
        String field = sortField.toLowerCase();
        if (!validFields.contains(field)) {
            log.warn("Некорректное поле сортировки: {}", sortField);
            return findAll();
        }

        String order = ascending ? "ASC" : "DESC";
        String query = "FROM Employee e ORDER BY e." + field + " " + order;

        return executeWithoutTransaction(session ->
                session.createQuery(query, Employee.class).getResultList()
        );
    }

    @Override
    public List<Employee> findAllPaginated(int offset, int limit) {
        validatePaginationParams(offset, limit);

        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e LEFT JOIN FETCH e.skills", Employee.class)
                        .setFirstResult(offset)
                        .setMaxResults(limit)
                        .getResultList()
        );
    }

    @Override
    public long count() {
        return executeWithoutTransaction(session ->
                session.createQuery("SELECT COUNT(*) FROM Employee", Long.class)
                        .getSingleResult()
        );
    }

    @Override
    public Optional<Employee> findById(Long id) {
        validateId(id);

        return executeWithoutTransaction(session ->
                Optional.ofNullable(session.createQuery(
                                "SELECT e FROM Employee e " +
                                        "LEFT JOIN FETCH e.skills " +
                                        "LEFT JOIN FETCH e.educations " +
                                        "WHERE e.id = :id", Employee.class)
                        .setParameter("id", id)
                        .uniqueResult())
        );
    }

    @Override
    public Optional<Employee> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }

        return executeWithoutTransaction(session ->
                Optional.ofNullable(session.createQuery(
                                "FROM Employee e WHERE e.email = :email", Employee.class)
                        .setParameter("email", email)
                        .uniqueResult())
        );
    }

    @Override
    public Employee save(Employee employee) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");

        return executeInTransaction(session -> {
            if (employee.getId() == null) {
                session.persist(employee);
            } else {
                session.merge(employee);
            }
            return employee;
        });
    }

    @Override
    public <S extends Employee> List<S> saveAll(Iterable<S> employees) {
        Objects.requireNonNull(employees, "Список сотрудников не может быть null");

        return executeInTransaction(session -> {
            List<S> saved = new ArrayList<>();
            for (S employee : employees) {
                if (employee.getId() == null) {
                    session.persist(employee);
                } else {
                    session.merge(employee);
                }
                saved.add(employee);
            }
            return saved;
        });
    }

    @Override
    public void deleteById(Long id) {
        validateId(id);

        executeInTransaction(session -> {
            Employee employee = session.get(Employee.class, id);
            if (employee != null) {
                session.remove(employee);
            }
            return null;
        });
    }

    @Override
    public void delete(Employee employee) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");

        executeInTransaction(session -> {
            if (session.contains(employee)) {
                session.remove(employee);
            } else {
                session.remove(session.merge(employee));
            }
            return null;
        });
    }

    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        Objects.requireNonNull(ids, "Список ID не может быть null");

        List<Long> idList = StreamSupport.stream(ids.spliterator(), false)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (idList.isEmpty()) {
            return;
        }

        executeInTransaction(session -> {
            session.createMutationQuery("DELETE FROM Employee e WHERE e.id IN :ids")
                    .setParameter("ids", idList)
                    .executeUpdate();
            return null;
        });
    }

    @Override
    public void deleteAll(Iterable<? extends Employee> employees) {
        Objects.requireNonNull(employees, "Список сотрудников не может быть null");

        List<Long> ids = StreamSupport.stream(employees.spliterator(), false)
                .filter(Objects::nonNull)
                .map(Employee::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        deleteAllById(ids);
    }

    @Override
    public void deleteAll() {
        executeInTransaction(session -> {
            session.createMutationQuery("DELETE FROM Employee").executeUpdate();
            return null;
        });
    }

    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            return false;
        }

        return executeWithoutTransaction(session ->
                session.createQuery("SELECT COUNT(e) FROM Employee e WHERE e.id = :id", Long.class)
                        .setParameter("id", id)
                        .getSingleResult() > 0
        );
    }

    @Override
    public List<Employee> findByNameContaining(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            return findAll();
        }

        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e WHERE LOWER(e.name) LIKE LOWER(:namePart)", Employee.class)
                        .setParameter("namePart", "%" + namePart + "%")
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findByNameContainingPaginated(String namePart, int offset, int limit) {
        validatePaginationParams(offset, limit);

        if (namePart == null || namePart.isBlank()) {
            return findAllPaginated(offset, limit);
        }

        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e LEFT JOIN FETCH e.skills WHERE LOWER(e.name) LIKE LOWER(:namePart)", Employee.class)
                        .setParameter("namePart", "%" + namePart + "%")
                        .setFirstResult(offset)
                        .setMaxResults(limit)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findBySkill(Skills skill) {
        if (skill == null) {
            return Collections.emptyList();
        }

        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e WHERE :skill MEMBER OF e.skills", Employee.class)
                        .setParameter("skill", skill)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findBySkills(List<Skills> skills) {
        if (skills == null || skills.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> skillNames = skills.stream()
                .map(Enum::name)
                .collect(Collectors.toList());

        return executeWithoutTransaction(session ->
                session.createQuery("SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE s IN :skillNames", Employee.class)
                        .setParameter("skillNames", skillNames)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findBySkillCategory(String category) {
        if (category == null || category.isBlank()) {
            return Collections.emptyList();
        }

        return executeWithoutTransaction(session ->
                session.createQuery("SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE s.category = :category", Employee.class)
                        .setParameter("category", category)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findByEmailContaining(String emailPart) {
        return List.of();
    }

    @Override
    public List<Employee> findByPhoneNumberContaining(String phonePart) {
        return List.of();
    }

    @Override
    public List<Employee> findBySchoolAndSkill(String school, Skills skill) {
        return List.of();
    }

    // === РЕАЛИЗАЦИЯ НОВЫХ МЕТОДОВ ===

    @Override
    public List<Employee> findByDeletedTrue() {
        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e WHERE e.deleted = true", Employee.class)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findActiveByNameContainingPaginated(String name, int offset, int limit) {
        validatePaginationParams(offset, limit);

        if (name == null || name.isBlank()) {
            return executeWithoutTransaction(session ->
                    session.createQuery("FROM Employee e WHERE e.deleted = false", Employee.class)
                            .setFirstResult(offset)
                            .setMaxResults(limit)
                            .getResultList()
            );
        }

        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e WHERE e.deleted = false AND LOWER(e.name) LIKE LOWER(:name)", Employee.class)
                        .setParameter("name", "%" + name + "%")
                        .setFirstResult(offset)
                        .setMaxResults(limit)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findAllActivePaginated(int offset, int limit) {
        validatePaginationParams(offset, limit);

        return executeWithoutTransaction(session ->
                session.createQuery("FROM Employee e WHERE e.deleted = false", Employee.class)
                        .setFirstResult(offset)
                        .setMaxResults(limit)
                        .getResultList()
        );
    }

    @Override
    public List<Employee> findByNameOrEmail(String name, String email) {
        if ((name == null || name.isBlank()) && (email == null || email.isBlank())) {
            return findAll();
        }

        return executeWithoutTransaction(session -> {
            String query = "FROM Employee e WHERE ";
            List<String> conditions = new ArrayList<>();
            Map<String, Object> parameters = new HashMap<>();

            if (name != null && !name.isBlank()) {
                conditions.add("LOWER(e.name) LIKE LOWER(:name)");
                parameters.put("name", "%" + name + "%");
            }

            if (email != null && !email.isBlank()) {
                conditions.add("LOWER(e.email) LIKE LOWER(:email)");
                parameters.put("email", "%" + email + "%");
            }

            query += String.join(" OR ", conditions);

            var hqlQuery = session.createQuery(query, Employee.class);
            parameters.forEach(hqlQuery::setParameter);

            return hqlQuery.getResultList();
        });
    }

    @Override
    public List<String> findAllDistinctSchools() {
        return executeWithoutTransaction(session ->
                session.createQuery("SELECT DISTINCT e.school FROM Employee e WHERE e.school IS NOT NULL", String.class)
                        .getResultList()
        );
    }

    @Override
    public int updateEmailById(Long id, String newEmail) {
        validateId(id);
        Objects.requireNonNull(newEmail, "Новый email не может быть null");

        if (newEmail.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }

        return executeInTransaction(session ->
                session.createMutationQuery("UPDATE Employee e SET e.email = :email WHERE e.id = :id")
                        .setParameter("email", newEmail)
                        .setParameter("id", id)
                        .executeUpdate()
        );
    }

    @Override
    public int deleteBySchool(String school) {
        Objects.requireNonNull(school, "Школа не может быть null");

        if (school.isBlank()) {
            throw new IllegalArgumentException("Школа не может быть пустой");
        }

        return executeInTransaction(session ->
                session.createMutationQuery("DELETE FROM Employee e WHERE e.school = :school")
                        .setParameter("school", school)
                        .executeUpdate()
        );
    }

    // === МЕТОДЫ ДЛЯ ОТЗЫВОВ ===

    @Override
    public void saveReview(Review review) {
        Objects.requireNonNull(review, "Отзыв не может быть null");
        Objects.requireNonNull(review.getEmployee(), "Сотрудник в отзыве не может быть null");

        if (review.getId() != null) {
            log.warn("Попытка сохранить Review с установленным id: {}. Сбрасываем id для persist", review.getId());
            review.setId(null);
        }

        executeInTransaction(session -> {
            session.persist(review);
            return null;
        });
    }

    @Override
    public List<Review> findReviewsByEmployeeId(Long employeeId) {
        validateId(employeeId);

        return executeWithoutTransaction(session ->
                session.createQuery("FROM Review r WHERE r.employee.id = :employeeId ORDER BY r.createdAt DESC", Review.class)
                        .setParameter("employeeId", employeeId)
                        .getResultList()
        );
    }

    @Override
    public long countByNameContaining(String name) {
        if (name == null || name.isBlank()) {
            return count();
        }

        return executeWithoutTransaction(session ->
                session.createQuery("SELECT COUNT(e) FROM Employee e WHERE LOWER(e.name) LIKE LOWER(:name)", Long.class)
                        .setParameter("name", "%" + name + "%")
                        .getSingleResult()
        );
    }

    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    private void validateId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Некорректный ID сотрудника: " + id);
        }
    }

    private void validatePaginationParams(int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            throw new IllegalArgumentException("Некорректные параметры пагинации: offset=" + offset + ", limit=" + limit);
        }
    }

    @PreDestroy
    @Override
    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            log.info("SessionFactory закрыт");
        }
    }

    // === ФУНКЦИОНАЛЬНЫЕ ИНТЕРФЕЙСЫ ДЛЯ ЛЯМБДА-ВЫРАЖЕНИЙ ===

    @FunctionalInterface
    private interface TransactionOperation<T> {
        T execute(Session session);
    }

    @FunctionalInterface
    private interface QueryOperation<T> {
        T execute(Session session);
    }
} */