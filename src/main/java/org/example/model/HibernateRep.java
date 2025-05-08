package org.example.model;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.example.EmployeeRepository;
import org.hibernate.Session;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
@Primary
@Transactional
public class HibernateRep implements EmployeeRepository {

    private final EntityManager entityManager;

    public HibernateRep(EntityManagerFactory entityManagerFactory) {
        this.entityManager = entityManagerFactory.createEntityManager();
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
        return Optional.ofNullable(getCurrentSession().get(Employee.class, id));
    }

    @Override
    public Employee save(Employee employee) {
        getCurrentSession().persist(employee);
        return employee;
    }

    @Override
    public void deleteById(Long id) {
        Employee employee = getCurrentSession().get(Employee.class, id);
        if (employee != null) {
            getCurrentSession().remove(employee);
        }
    }

    @Override
    public void delete(Employee employee) {
        getCurrentSession().remove(employee);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(Long id) {
        return getCurrentSession().createQuery(
                        "SELECT COUNT(e) > 0 FROM Employee e WHERE e.id = :id", Boolean.class)
                .setParameter("id", id)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByNameContaining(String namePart) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE e.name LIKE :namePart", Employee.class)
                .setParameter("namePart", "%" + namePart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySkill(Skills skill) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE :skill MEMBER OF e.skills", Employee.class)
                .setParameter("skill", skill)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByEmailContaining(String emailPart) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE e.email LIKE :emailPart", Employee.class)
                .setParameter("emailPart", "%" + emailPart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public long count() {
        return getCurrentSession().createQuery("SELECT COUNT(e) FROM Employee e", Long.class)
                .getSingleResult();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByNameContainingIgnoreCase(String namePart) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE LOWER(e.name) LIKE LOWER(:namePart)", Employee.class)
                .setParameter("namePart", "%" + namePart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByEmailContainingIgnoreCase(String emailPart) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE LOWER(e.email) LIKE LOWER(:emailPart)", Employee.class)
                .setParameter("emailPart", "%" + emailPart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySkillsContaining(Skills skill) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE :skill MEMBER OF e.skills", Employee.class)
                .setParameter("skill", skill)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByPhoneNumberContaining(String phonePart) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE e.phoneNumber LIKE :phonePart", Employee.class)
                .setParameter("phonePart", "%" + phonePart + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findAll(Pageable pageable) {
        long total = count();
        List<Employee> result = getCurrentSession().createQuery(
                        "SELECT e FROM Employee e", Employee.class)
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();
        return new PageImpl<>(result, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<Employee> findByNameContaining(String namePart, Pageable pageable) {
        long total = getCurrentSession().createQuery(
                        "SELECT COUNT(e) FROM Employee e WHERE e.name LIKE :namePart", Long.class)
                .setParameter("namePart", "%" + namePart + "%")
                .getSingleResult();

        List<Employee> result = getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE e.name LIKE :namePart", Employee.class)
                .setParameter("namePart", "%" + namePart + "%")
                .setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        return new PageImpl<>(result, pageable, total);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findBySchoolAndSkills(String school, Skills skill) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE e.school LIKE :school " +
                                "AND :skill MEMBER OF e.skills", Employee.class)
                .setParameter("school", "%" + school + "%")
                .setParameter("skill", skill)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByNameOrEmail(String name, String email) {
        return getCurrentSession().createQuery(
                        "SELECT e FROM Employee e WHERE e.name LIKE :name " +
                                "OR e.email LIKE :email", Employee.class)
                .setParameter("name", "%" + name + "%")
                .setParameter("email", "%" + email + "%")
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public <T> List<T> findBy(Class<T> type) {
        if (type == String.class) {
            return (List<T>) getCurrentSession().createQuery(
                            "SELECT e.name FROM Employee e", String.class)
                    .getResultList();
        }
        throw new UnsupportedOperationException("Unsupported projection type: " + type.getName());
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> findAllDistinctSchools() {
        return getCurrentSession().createQuery(
                        "SELECT DISTINCT e.school FROM Employee e WHERE e.school IS NOT NULL", String.class)
                .getResultList();
    }

    @Override
    @Transactional
    public int updateEmailById(Long id, String newEmail) {
        return getCurrentSession().createMutationQuery(
                        "UPDATE Employee e SET e.email = :email WHERE e.id = :id")
                .setParameter("email", newEmail)
                .setParameter("id", id)
                .executeUpdate();
    }

    @Override
    @Transactional
    public int deleteBySchool(String school) {
        return getCurrentSession().createMutationQuery(
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
        return getCurrentSession().createQuery(
                        "SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE s IN :skills", Employee.class)
                .setParameter("skills", skills)
                .getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Employee> findByCategory(String category) {
        if (category == null || category.isEmpty()) {
            return findAll();
        }
        return getCurrentSession().createQuery(
                        "SELECT DISTINCT e FROM Employee e JOIN e.skills s WHERE s.category = :category", Employee.class)
                .setParameter("category", category)
                .getResultList();
    }
}