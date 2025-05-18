package org.example.repository;

import lombok.extern.slf4j.Slf4j;
import org.example.model.Education;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@Transactional
public class EducationRepository {
    private final SessionFactory sessionFactory;

    @Autowired
    public EducationRepository(SessionFactory sessionFactory) {
        this.sessionFactory = Objects.requireNonNull(sessionFactory, "SessionFactory cannot be null");
    }

    protected Session getCurrentSession() {
        return sessionFactory.getCurrentSession();
    }

    public List<Education> findByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            log.warn("Попытка найти образование с null ID сотрудника");
            return Collections.emptyList();
        }
        log.debug("Получение образования для сотрудника ID: {}", employeeId);
        return getCurrentSession()
                .createQuery("FROM Education e WHERE e.employee.id = :employeeId", Education.class)
                .setParameter("employeeId", employeeId)
                .getResultList();
    }

    public void save(Education education) {
        Objects.requireNonNull(education, "Образование не может быть null");
        log.info("Сохранение образования для сотрудника ID: {}", education.getEmployee().getId());
        getCurrentSession().persist(education);
    }

    public void deleteByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            log.warn("Попытка удалить образование с null ID сотрудника");
            return;
        }
        log.info("Удаление образования для сотрудника ID: {}", employeeId);
        getCurrentSession()
                .createMutationQuery("DELETE FROM Education e WHERE e.employee.id = :employeeId")
                .setParameter("employeeId", employeeId)
                .executeUpdate();
    }
}