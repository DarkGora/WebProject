package org.example.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.example.repository.EmployeeRepository;
import org.hibernate.Session;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
@Primary
@Transactional
public class HibernateRep implements EmployeeRepository {

    private final EntityManager entityManager;

    public HibernateRep(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    protected Session getCurrentSession() {
        return entityManager.unwrap(Session.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll() {
        return getCurrentSession().createQuery("FROM Employee", Employee.class).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Employee> findById(Long id) {
        return Optional.ofNullable(getCurrentSession().find(Employee.class, id));
    }

    @Override
    @Transactional
    public Employee save(Employee employee) {
        if (employee == null) {
            throw new IllegalArgumentException("Employee cannot be null");
        }

        // Проверка уникальности email
        if (employee.getEmail() != null) {
            TypedQuery<Long> query = entityManager.createQuery(
                    "SELECT COUNT(e) FROM Employee e WHERE e.email = :email AND (e.id != :id OR :id IS NULL)", Long.class);
            query.setParameter("email", employee.getEmail());
            query.setParameter("id", employee.getId());
            Long count = query.getSingleResult();
            if (count > 0) {
                throw new DataIntegrityViolationException("Email " + employee.getEmail() + " already exists");
            }
        }

        if (employee.getId() == null) {
            entityManager.persist(employee);
            return employee;
        } else {
            return entityManager.merge(employee);
        }
    }

    @Override
    @Transactional
    public <S extends Employee> List<S> saveAll(Iterable<S> entities) {
        if (entities == null) {
            return Collections.emptyList();
        }
        List<S> result = new ArrayList<>();
        for (S entity : entities) {
            result.add((S) save(entity));
        }
        return result;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        Employee employee = findById(id).orElse(null);
        if (employee != null) {
            entityManager.remove(employee);
        }
    }

    @Override
    @Transactional
    public void delete(Employee employee) {
        if (employee != null) {
            if (entityManager.contains(employee)) {
                entityManager.remove(employee);
            } else {
                Employee managedEmployee = entityManager.merge(employee);
                entityManager.remove(managedEmployee);
            }
        }
    }

    @Override
    @Transactional
    public void deleteAllById(Iterable<? extends Long> ids) {
        if (ids != null) {
            for (Long id : ids) {
                deleteById(id);
            }
        }
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<? extends Employee> entities) {
        if (entities != null) {
            for (Employee entity : entities) {
                delete(entity);
            }
        }
    }

    @Override
    @Transactional
    public void deleteAll() {
        entityManager.createQuery("DELETE FROM Employee").executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM Employee e WHERE e.id = :id", Long.class)
                .setParameter("id", id)
                .getSingleResult() > 0;
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
    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String namePart) {
        return createQueryWithLike("name", namePart).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySkill(Skills skill) {
        return findBySkillsContaining(skill);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySkillsContaining(Skills skill) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> root = cq.from(Employee.class);

        cq.select(root).where(cb.isMember(skill, root.get("skills")));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByEmailContaining(String emailPart) {
        return createQueryWithLike("email", emailPart).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return entityManager.createQuery("SELECT COUNT(e) FROM Employee e", Long.class)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByNameContainingIgnoreCase(String namePart) {
        return createQueryWithLikeIgnoreCase("name", namePart).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByEmailContainingIgnoreCase(String emailPart) {
        return createQueryWithLikeIgnoreCase("email", emailPart).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByPhoneNumberContaining(String phonePart) {
        return createQueryWithLike("phoneNumber", phonePart).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findByNameContaining(String namePart, Pageable pageable) {
        TypedQuery<Employee> query = createQueryWithLike("name", namePart);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        long total = countByNameContaining(namePart);
        List<Employee> content = query.getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySchoolAndSkill(String school, Skills skill) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> root = cq.from(Employee.class);

        Predicate schoolPredicate = cb.equal(root.get("school"), school);
        Predicate skillPredicate = cb.isMember(skill, root.get("skills"));

        cq.select(root).where(cb.and(schoolPredicate, skillPredicate));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findAll(Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> root = cq.from(Employee.class);

        // Применяем сортировку
        if (pageable.getSort().isSorted()) {
            List<Order> orders = pageable.getSort().stream()
                    .map(order -> order.isAscending() ?
                            cb.asc(root.get(order.getProperty())) :
                            cb.desc(root.get(order.getProperty())))
                    .collect(Collectors.toList());
            cq.orderBy(orders);
        }

        TypedQuery<Employee> query = entityManager.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        long total = count();
        List<Employee> content = query.getResultList();

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySchoolAndSkills(String school, Skills skill) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> root = cq.from(Employee.class);

        Predicate schoolPredicate = cb.like(root.get("school"), "%" + school + "%");
        Predicate skillPredicate = cb.isMember(skill, root.get("skills"));

        cq.select(root).where(cb.and(schoolPredicate, skillPredicate));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByNameOrEmail(String name, String email) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> root = cq.from(Employee.class);

        Predicate namePredicate = cb.like(root.get("name"), "%" + name + "%");
        Predicate emailPredicate = cb.like(root.get("email"), "%" + email + "%");

        cq.select(root).where(cb.or(namePredicate, emailPredicate));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByCategory(String category) {
        return entityManager.createQuery(
                        "SELECT e FROM Employee e WHERE e.category = :category", Employee.class)
                .setParameter("category", category)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public <T> List<T> findBy(Class<T> type) {
        if (Employee.class.isAssignableFrom(type)) {
            return (List<T>) findAll();
        }
        return Collections.emptyList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllDistinctSchools() {
        return entityManager.createQuery(
                        "SELECT DISTINCT e.school FROM Employee e WHERE e.school IS NOT NULL", String.class)
                .getResultList();
    }

    @Override
    @Transactional
    public int updateEmailById(Long id, String newEmail) {
        return entityManager.createQuery(
                        "UPDATE Employee e SET e.email = :email WHERE e.id = :id")
                .setParameter("email", newEmail)
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int deleteBySchool(String school) {
        return entityManager.createQuery(
                        "DELETE FROM Employee e WHERE e.school = :school")
                .setParameter("school", school)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySkillsIn(List<Skills> skills) {
        if (skills == null || skills.isEmpty()) {
            return findAll();
        }

        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> root = cq.from(Employee.class);

        cq.select(root).where(root.join("skills").in(skills));
        return entityManager.createQuery(cq).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAllById(Iterable<Long> ids) {
        if (ids == null || !ids.iterator().hasNext()) {
            return Collections.emptyList();
        }
        return entityManager.createQuery(
                        "SELECT e FROM Employee e WHERE e.id IN :ids", Employee.class)
                .setParameter("ids", toList(ids))
                .getResultList();
    }

    @Override
    @Transactional
    public void flush() {
        entityManager.flush();
    }

    @Override
    @Transactional
    public <S extends Employee> S saveAndFlush(S entity) {
        S saved = (S) save(entity);
        entityManager.flush();
        return saved;
    }

    @Override
    @Transactional
    public <S extends Employee> List<S> saveAllAndFlush(Iterable<S> entities) {
        List<S> saved = saveAll(entities);
        entityManager.flush();
        return saved;
    }

    @Override
    @Transactional
    public void deleteAllInBatch(Iterable<Employee> entities) {
        if (entities == null || !entities.iterator().hasNext()) {
            return;
        }
        List<Long> ids = toList(entities).stream()
                .map(Employee::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (!ids.isEmpty()) {
            entityManager.createQuery("DELETE FROM Employee e WHERE e.id IN :ids")
                    .setParameter("ids", ids)
                    .executeUpdate();
        }
    }

    @Override
    @Transactional
    public void deleteAllByIdInBatch(Iterable<Long> ids) {
        if (ids == null || !ids.iterator().hasNext()) {
            return;
        }
        entityManager.createQuery("DELETE FROM Employee e WHERE e.id IN :ids")
                .setParameter("ids", toList(ids))
                .executeUpdate();
    }

    @Override
    @Transactional
    public void deleteAllInBatch() {
        entityManager.createQuery("DELETE FROM Employee").executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getOne(Long id) {
        if (id == null) {
            return null;
        }
        return entityManager.getReference(Employee.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getById(Long id) {
        return findById(id).orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Employee getReferenceById(Long id) {
        return entityManager.getReference(Employee.class, id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findAll(Sort sort) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Employee> cq = cb.createQuery(Employee.class);
        Root<Employee> root = cq.from(Employee.class);

        if (sort.isSorted()) {
            List<Order> orders = sort.stream()
                    .map(order -> order.isAscending() ?
                            cb.asc(root.get(order.getProperty())) :
                            cb.desc(root.get(order.getProperty())))
                    .collect(Collectors.toList());
            cq.orderBy(orders);
        }

        return entityManager.createQuery(cq).getResultList();
    }

    // Вспомогательные методы
    private TypedQuery<Employee> createQueryWithLike(String field, String value) {
        return entityManager.createQuery(
                        "SELECT e FROM Employee e WHERE e." + field + " LIKE :value", Employee.class)
                .setParameter("value", "%" + value + "%");
    }

    private TypedQuery<Employee> createQueryWithLikeIgnoreCase(String field, String value) {
        return entityManager.createQuery(
                        "SELECT e FROM Employee e WHERE LOWER(e." + field + ") LIKE LOWER(:value)", Employee.class)
                .setParameter("value", "%" + value + "%");
    }

    private long countByNameContaining(String namePart) {
        return entityManager.createQuery(
                        "SELECT COUNT(e) FROM Employee e WHERE e.name LIKE :name", Long.class)
                .setParameter("name", "%" + namePart + "%")
                .getSingleResult();
    }

    private <T> List<T> toList(Iterable<T> iterable) {
        List<T> result = new ArrayList<>();
        iterable.forEach(result::add);
        return result;
    }

    // Методы для поддержки Example-запросов (оставлены без реализации, если не требуются)
    @Override
    public <S extends Employee> Optional<S> findOne(Example<S> example) {
        return Optional.empty(); // Требуется реализация с Criteria API или Spring Data JPA
    }

    @Override
    public <S extends Employee> List<S> findAll(Example<S> example) {
        return Collections.emptyList(); // Требуется реализация
    }

    @Override
    public <S extends Employee> List<S> findAll(Example<S> example, Sort sort) {
        return Collections.emptyList(); // Требуется реализация
    }

    @Override
    public <S extends Employee> Page<S> findAll(Example<S> example, Pageable pageable) {
        return new PageImpl<>(Collections.emptyList(), pageable, 0); // Требуется реализация
    }

    @Override
    public <S extends Employee> long count(Example<S> example) {
        return 0; // Требуется реализация
    }

    @Override
    public <S extends Employee> boolean exists(Example<S> example) {
        return false; // Требуется реализация
    }

    @Override
    public <S extends Employee, R> R findBy(Example<S> example, Function<FluentQuery.FetchableFluentQuery<S>, R> queryFunction) {
        return null; // Требуется реализация
    }
}