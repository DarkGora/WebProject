package org.example.repository;

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
@Slf4j
@Repository
public class HibernateRep implements EmployeeRepository, AutoCloseable {
    private final SessionFactory sessionFactory;

    public HibernateRep() {
        try {
            Configuration configuration = new Configuration()
                    .configure()
                    // Добавляем сканирование пакета с сущностями
                    .addPackage("org.example.model")
                    .addAnnotatedClass(Employee.class)
                    .addAnnotatedClass(Review.class)
                    .addAnnotatedClass(Skills.class) // Если используется
                    .addAnnotatedClass(Education.class); // Если используется
            this.sessionFactory = configuration.buildSessionFactory();
            log.info("SessionFactory успешно создан");
        } catch (Exception e) {
            log.error("Ошибка при создании SessionFactory", e);
            throw new RuntimeException("Не удалось инициализировать Hibernate", e);
        }
    }

    /**
     * Получение сессии Hibernate.
     *
     * @return открытая сессия
     * @throws IllegalStateException если сессия недоступна
     */
    private Session getSession() {
        try {
            return sessionFactory.openSession();
        } catch (Exception e) {
            log.error("Ошибка при получении сессии Hibernate", e);
            throw new IllegalStateException("Не удалось получить сессию", e);
        }
    }

    /**
     * Поиск всех сотрудников.
     *
     * @return список сотрудников или пустой список при ошибке
     */
    @Override
    public List<Employee> findAll() {
        Session session = getSession();
        try {
            return session.createQuery("FROM Employee", Employee.class).getResultList();
        } catch (Exception e) {
            log.error("Ошибка при получении всех сотрудников", e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findAll");
            }
        }
    }

    /**
     * Поиск сотрудников с сортировкой.
     *
     * @param sortField поле для сортировки (name, email, phoneNumber, school, createdAt)
     * @param ascending направление сортировки (true для ASC, false для DESC)
     * @return отсортированный список сотрудников или все сотрудники, если поле некорректно
     */
    @Override
    public List<Employee> findAllSorted(String sortField, boolean ascending) {
        if (sortField == null || sortField.isBlank()) {
            log.debug("Поле сортировки не указано, возвращаются все сотрудники");
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
        Session session = getSession();
        try {
            return session.createQuery(query, Employee.class).getResultList();
        } catch (Exception e) {
            log.error("Ошибка при получении сотрудников с сортировкой по полю {}: {}", sortField, e.getMessage());
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findAllSorted");
            }
        }
    }

    /**
     * Поиск сотрудников с пагинацией.
     *
     * @param offset начальная позиция
     * @param limit  количество записей
     * @return список сотрудников или пустой список при ошибке
     */
    public List<Employee> findAllPaginated(int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            log.warn("Некорректные параметры пагинации: offset={}, limit={}", offset, limit);
            throw new IllegalArgumentException("Некорректные параметры пагинации");
        }
        Session session = getSession();
        try {
            return session.createQuery(
                            "SELECT e FROM Employee e LEFT JOIN FETCH e.skills",
                            Employee.class)
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при получении сотрудников с пагинацией: offset={}, limit={}", offset, limit, e);
            throw new RuntimeException("Не удалось загрузить сотрудников с пагинацией", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findAllPaginated");
            }
        }
    }

    /**
     * Подсчет всех сотрудников.
     *
     * @return количество сотрудников или 0 при ошибке
     */
    @Override
    public long count() {
        Session session = getSession();
        try {
            return session.createQuery("SELECT COUNT(*) FROM Employee", Long.class)
                    .getSingleResult();
        } catch (Exception e) {
            log.error("Ошибка при подсчете сотрудников", e);
            return 0L;
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для count");
            }
        }
    }

    /**
     * Поиск сотрудника по идентификатору.
     *
     * @param id идентификатор сотрудника
     * @return Optional с сотрудником или пустой, если не найден
     */
    public Optional<Employee> findById(Long id) {
        if (id == null || id <= 0) {
            log.warn("Некорректный ID сотрудника: {}", id);
            throw new IllegalArgumentException("Некорректный ID сотрудника");
        }
        Session session = getSession();
        try {
            return Optional.ofNullable(session.createQuery(
                            "SELECT e FROM Employee e LEFT JOIN FETCH e.skills LEFT JOIN FETCH e.educations WHERE e.id = :id",
                            Employee.class)
                    .setParameter("id", id)
                    .uniqueResult());
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудника по ID: {}", id, e);
            throw new RuntimeException("Не удалось загрузить сотрудника с ID: " + id, e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findById");
            }
        }
    }

    /**
     * Поиск сотрудника по email.
     *
     * @param email адрес электронной почты
     * @return Optional с сотрудником или пустой, если не найден
     */
    @Override
    public Optional<Employee> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("Попытка найти сотрудника с пустым email");
            return Optional.empty();
        }
        Session session = getSession();
        try {
            Employee employee = session.createQuery("FROM Employee e WHERE e.email = :email", Employee.class)
                    .setParameter("email", email)
                    .uniqueResult();
            return Optional.ofNullable(employee);
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудника по email: {}", email, e);
            return Optional.empty();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByEmail");
            }
        }
    }

    /**
     * Сохранение сотрудника.
     *
     * @param employee объект сотрудника
     * @return сохраненный сотрудник
     * @throws IllegalArgumentException если сотрудник null
     */
    @Override
    public Employee save(Employee employee) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        Session session = getSession();
        try {
            session.beginTransaction();
            if (employee.getId() == null) {
                session.persist(employee);
            } else {
                session.merge(employee);
            }
            session.getTransaction().commit();
            return employee;
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для save");
            }
            log.error("Ошибка при сохранении сотрудника: {}", employee.getName(), e);
            throw new RuntimeException("Не удалось сохранить сотрудника", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для save");
            }
        }
    }

    /**
     * Массовое сохранение сотрудников.
     *
     * @param employees список сотрудников
     * @return список сохраненных сотрудников
     * @throws IllegalArgumentException если список null
     */
    @Override
    public <S extends Employee> List<S> saveAll(Iterable<S> employees) {
        Objects.requireNonNull(employees, "Список сотрудников не может быть null");
        Session session = getSession();
        try {
            session.beginTransaction();
            List<S> saved = new ArrayList<>();
            for (S employee : employees) {
                if (employee.getId() == null) {
                    session.persist(employee);
                } else {
                    session.merge(employee);
                }
                saved.add(employee);
            }
            session.getTransaction().commit();
            return saved;
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для saveAll");
            }
            log.error("Ошибка при массовом сохранении сотрудников", e);
            throw new RuntimeException("Не удалось сохранить сотрудников", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для saveAll");
            }
        }
    }

    /**
     * Удаление сотрудника по ID.
     *
     * @param id идентификатор сотрудника
     * @throws IllegalArgumentException если ID null
     */
    @Override
    public void deleteById(Long id) {
        Objects.requireNonNull(id, "ID сотрудника не может быть null");
        Session session = getSession();
        try {
            session.beginTransaction();
            Employee employee = session.get(Employee.class, id);
            if (employee != null) {
                session.remove(employee);
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для deleteById");
            }
            log.error("Ошибка при удалении сотрудника с ID: {}", id, e);
            throw new RuntimeException("Не удалось удалить сотрудника", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для deleteById");
            }
        }
    }

    /**
     * Удаление сотрудника.
     *
     * @param employee объект сотрудника
     * @throws IllegalArgumentException если сотрудник null
     */
    @Override
    public void delete(Employee employee) {
        Objects.requireNonNull(employee, "Сотрудник не может быть null");
        Session session = getSession();
        try {
            session.beginTransaction();
            if (session.contains(employee)) {
                session.remove(employee);
            } else {
                session.remove(session.merge(employee));
            }
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для delete");
            }
            log.error("Ошибка при удалении сотрудника: {}", employee.getName(), e);
            throw new RuntimeException("Не удалось удалить сотрудника", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для delete");
            }
        }
    }

    /**
     * Удаление сотрудников по списку ID.
     *
     * @param ids список идентификаторов
     * @throws IllegalArgumentException если список null
     */
    @Override
    public void deleteAllById(Iterable<? extends Long> ids) {
        Objects.requireNonNull(ids, "Список ID не может быть null");
        List<Long> idList = StreamSupport.stream(ids.spliterator(), false)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (idList.isEmpty()) {
            log.debug("Список ID пуст, удаление не выполняется");
            return;
        }
        Session session = getSession();
        try {
            session.beginTransaction();
            session.createMutationQuery("DELETE FROM Employee e WHERE e.id IN :ids")
                    .setParameter("ids", idList)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для deleteAllById");
            }
            log.error("Ошибка при массовом удалении сотрудников по ID", e);
            throw new RuntimeException("Не удалось удалить сотрудников", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для deleteAllById");
            }
        }
    }

    /**
     * Удаление списка сотрудников.
     *
     * @param employees список сотрудников
     * @throws IllegalArgumentException если список null
     */
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

    /**
     * Удаление всех сотрудников.
     */
    @Override
    public void deleteAll() {
        Session session = getSession();
        try {
            session.beginTransaction();
            session.createMutationQuery("DELETE FROM Employee").executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для deleteAll");
            }
            log.error("Ошибка при удалении всех сотрудников", e);
            throw new RuntimeException("Не удалось удалить всех сотрудников", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для deleteAll");
            }
        }
    }

    /**
     * Проверка существования сотрудника по ID.
     *
     * @param id идентификатор сотрудника
     * @return true, если сотрудник существует, иначе false
     */
    @Override
    public boolean existsById(Long id) {
        if (id == null) {
            log.warn("Попытка проверить существование сотрудника с null ID");
            return false;
        }
        log.debug("Проверка существования сотрудника с ID: {}", id);
        Session session = getSession();
        try {
            return session.createQuery("SELECT COUNT(e) FROM Employee e WHERE e.id = :id", Long.class)
                    .setParameter("id", id)
                    .getSingleResult() > 0;
        } catch (Exception e) {
            log.error("Ошибка при проверке существования сотрудника с ID {}: {}", id, e.getMessage(), e);
            return false;
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для existsById");
            }
        }
    }

    /**
     * Поиск сотрудников по части имени.
     *
     * @param namePart часть имени
     * @return список сотрудников или все сотрудники, если namePart пустой
     */
    @Override
    public List<Employee> findByNameContaining(String namePart) {
        if (namePart == null || namePart.isBlank()) {
            log.debug("Часть имени не указана, возвращаются все сотрудники");
            return findAll();
        }
        Session session = getSession();
        try {
            return session.createQuery("FROM Employee e WHERE LOWER(e.name) LIKE LOWER(:namePart)", Employee.class)
                    .setParameter("namePart", "%" + namePart + "%")
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по имени: {}", namePart, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByNameContaining");
            }
        }
    }


    @Override
    public List<Employee> findByNameContainingPaginated(String namePart, int offset, int limit) {
        if (offset < 0 || limit <= 0) {
            log.warn("Некорректные параметры пагинации: offset={}, limit={}", offset, limit);
            throw new IllegalArgumentException("Некорректные параметры пагинации");
        }
        if (namePart == null || namePart.isBlank()) {
            return findAllPaginated(offset, limit);
        }
        Session session = getSession();
        try {
            List<Employee> employees = session.createQuery(
                            "FROM Employee e LEFT JOIN FETCH e.skills WHERE LOWER(e.name) LIKE LOWER(:namePart)", Employee.class)
                    .setParameter("namePart", "%" + namePart + "%")
                    .setFirstResult(offset)
                    .setMaxResults(limit)
                    .getResultList();
            // Гарантируем, что skills не null
            employees.forEach(e -> {
                if (e.getSkills() == null) {
                    e.setSkills(new HashSet<>());
                }
            });
            return employees;
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по имени с пагинацией: {}", namePart, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByNameContainingPaginated");
            }
        }
    }

    @Override
    public List<Employee> findBySkill(Skills skill) {
        if (skill == null) {
            log.warn("Попытка поиска сотрудников с null навыком");
            return Collections.emptyList();
        }
        Session session = getSession();
        try {
            return session.createQuery("FROM Employee e WHERE :skill MEMBER OF e.skills", Employee.class)
                    .setParameter("skill", skill)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по навыку: {}", skill, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findBySkill");
            }
        }
    }

    @Override
    public List<Employee> findBySkills(List<Skills> skills) {
        if (skills == null || skills.isEmpty()) {
            log.warn("Попытка поиска сотрудников с пустым списком навыков");
            return Collections.emptyList();
        }
        Session session = getSession();
        try {
            return session.createQuery("FROM Employee e JOIN e.skills s WHERE s IN :skills", Employee.class)
                    .setParameter("skills", skills)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по списку навыков: {}", skills, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findBySkills");
            }
        }
    }


    @Override
    public List<Employee> findBySkillCategory(String category) {
        if (category == null || category.isBlank()) {
            log.warn("Попытка поиска сотрудников с пустой категорией навыков");
            return Collections.emptyList();
        }
        Session session = getSession();
        try {
            return session.createQuery("FROM Employee e JOIN e.skills s WHERE s.category = :category", Employee.class)
                    .setParameter("category", category)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по категории навыков: {}", category, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findBySkillCategory");
            }
        }
    }


    @Override
    public List<Employee> findByEmailContaining(String emailPart) {
        if (emailPart == null || emailPart.isBlank()) {
            log.debug("Часть email не указана, возвращаются все сотрудники");
            return findAll();
        }
        Session session = getSession();
        try {
            return session.createQuery("FROM Employee e WHERE LOWER(e.email) LIKE LOWER(:emailPart)", Employee.class)
                    .setParameter("emailPart", "%" + emailPart + "%")
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по email: {}", emailPart, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByEmailContaining");
            }
        }
    }


    @Override
    public List<Employee> findByPhoneNumberContaining(String phonePart) {
        if (phonePart == null || phonePart.isBlank()) {
            log.debug("Часть номера телефона не указана, возвращаются все сотрудники");
            return findAll();
        }
        Session session = getSession();
        try {
            return session.createQuery("FROM Employee e WHERE e.phoneNumber LIKE :phonePart", Employee.class)
                    .setParameter("phonePart", "%" + phonePart + "%")
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по номеру телефона: {}", phonePart, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByPhoneNumberContaining");
            }
        }
    }


    @Override
    public List<Employee> findBySchoolAndSkill(String school, Skills skill) {
        if ((school == null || school.isBlank()) && skill == null) {
            log.debug("Школа и навык не указаны, возвращаются все сотрудники");
            return findAll();
        }
        StringBuilder query = new StringBuilder("FROM Employee e WHERE 1=1");
        if (school != null && !school.isBlank()) {
            query.append(" AND e.school LIKE :school");
        }
        if (skill != null) {
            query.append(" AND :skill MEMBER OF e.skills");
        }
        Session session = getSession();
        try {
            var hqlQuery = session.createQuery(query.toString(), Employee.class);
            if (school != null && !school.isBlank()) {
                hqlQuery.setParameter("school", "%" + school + "%");
            }
            if (skill != null) {
                hqlQuery.setParameter("skill", skill);
            }
            return hqlQuery.getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по школе и навыку: school={}, skill={}", school, skill, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findBySchoolAndSkill");
            }
        }
    }
    @Override
    public void saveReview(Review review) {
        Objects.requireNonNull(review, "Отзыв не может быть null");
        Objects.requireNonNull(review.getEmployeeId(), "ID сотрудника в отзыве не может быть null");
        if (review.getId() != null) {
            log.warn("Попытка сохранить Review с установленным id: {}. Сбрасываем id для persist", review.getId());
            review.setId(null); // Сбрасываем id, чтобы persist работал как для нового объекта
        }
        log.info("Сохранение отзыва в БД: id={}, employeeId={}, rating={}, comment={}",
                review.getId(), review.getEmployeeId(), review.getRating(), review.getComment());
        Session session = getSession();
        try {
            session.beginTransaction();
            session.persist(review);
            session.getTransaction().commit();
            log.debug("Отзыв успешно сохранен в БД");
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для saveReview");
            }
            log.error("Ошибка при сохранении отзыва для сотрудника ID {}: {}",
                    review.getEmployeeId(), e.getMessage(), e);
            throw new RuntimeException("Не удалось сохранить отзыв в БД", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для saveReview");
            }
        }
    }

    @Override
    public List<Review> findReviewsByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            log.warn("Попытка найти отзывы с null employeeId");
            return Collections.emptyList();
        }
        log.debug("Поиск отзывов для сотрудника ID: {}", employeeId);
        Session session = getSession();
        try {
            return session.createQuery("FROM Review r WHERE r.employeeId = :employeeId ORDER BY r.createdAt DESC", Review.class)
                    .setParameter("employeeId", employeeId)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске отзывов для сотрудника ID {}: {}", employeeId, e.getMessage(), e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findReviewsByEmployeeId");
            }
        }
    }

    @Override
    public List<Employee> findByNameOrEmail(String name, String email) {
        if ((name == null || name.isBlank()) && (email == null || email.isBlank())) {
            log.debug("Имя и email не указаны, возвращаются все сотрудники");
            return findAll();
        }
        Session session = getSession();
        try {
            return session.createQuery(
                            "FROM Employee e WHERE (:name IS NULL OR LOWER(e.name) LIKE LOWER(:name)) OR " +
                                    "(:email IS NULL OR LOWER(e.email) LIKE LOWER(:email))", Employee.class)
                    .setParameter("name", name != null ? "%" + name + "%" : null)
                    .setParameter("email", email != null ? "%" + email + "%" : null)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске сотрудников по имени или email: name={}, email={}", name, email, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByNameOrEmail");
            }
        }
    }


    @Override
    public List<String> findAllDistinctSchools() {
        Session session = getSession();
        try {
            return session.createQuery("SELECT DISTINCT e.school FROM Employee e WHERE e.school IS NOT NULL", String.class)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при получении уникальных школ", e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findAllDistinctSchools");
            }
        }
    }

    @Override
    public int updateEmailById(Long id, String newEmail) {
        Objects.requireNonNull(id, "ID сотрудника не может быть null");
        Objects.requireNonNull(newEmail, "Новый email не может быть null");
        if (newEmail.isBlank()) {
            log.warn("Попытка обновления email с пустым значением для ID: {}", id);
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        Session session = getSession();
        try {
            session.beginTransaction();
            int updated = session.createMutationQuery("UPDATE Employee e SET e.email = :email WHERE e.id = :id")
                    .setParameter("email", newEmail)
                    .setParameter("id", id)
                    .executeUpdate();
            session.getTransaction().commit();
            return updated;
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для updateEmailById");
            }
            log.error("Ошибка при обновлении email для сотрудника с ID: {}", id, e);
            throw new RuntimeException("Не удалось обновить email", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для updateEmailById");
            }
        }
    }


    @Override
    public int deleteBySchool(String school) {
        Objects.requireNonNull(school, "Школа не может быть null");
        if (school.isBlank()) {
            log.warn("Попытка удаления сотрудников с пустой школой");
            throw new IllegalArgumentException("Школа не может быть пустой");
        }
        Session session = getSession();
        try {
            session.beginTransaction();
            int deleted = session.createMutationQuery("DELETE FROM Employee e WHERE e.school = :school")
                    .setParameter("school", school)
                    .executeUpdate();
            session.getTransaction().commit();
            return deleted;
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для deleteBySchool");
            }
            log.error("Ошибка при удалении сотрудников по школе: {}", school, e);
            throw new RuntimeException("Не удалось удалить сотрудников", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для deleteBySchool");
            }
        }
    }

    /**
     * Закрытие SessionFactory при уничтожении бина.
     */
    @PreDestroy
    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            log.info("SessionFactory закрыт");
        }
    }
}