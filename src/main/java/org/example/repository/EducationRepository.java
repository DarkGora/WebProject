package org.example.repository;

import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Education;
import org.example.model.Employee;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.springframework.stereotype.Repository;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Slf4j
@Repository
public class EducationRepository {
    private final SessionFactory sessionFactory;

    public EducationRepository() {
        try {
            this.sessionFactory = new Configuration()
                    .configure()
                    .buildSessionFactory();
            log.info("SessionFactory успешно создан для EducationRepository");
        } catch (Exception e) {
            log.error("Ошибка при создании SessionFactory", e);
            throw new RuntimeException("Не удалось инициализировать Hibernate", e);
        }
    }

    private Session getSession() {
        try {
            return sessionFactory.openSession();
        } catch (Exception e) {
            log.error("Ошибка при получении сессии Hibernate", e);
            throw new IllegalStateException("Не удалось получить сессию", e);
        }
    }

    /**
     * Найти все образования сотрудника (только для активных сотрудников)
     */
    public List<Education> findByEmployeeId(@NotNull Long employeeId) {
        if (employeeId == null) {
            log.warn("Попытка найти образование с null ID сотрудника");
            return Collections.emptyList();
        }
        log.debug("Получение образования для сотрудника ID: {}", employeeId);
        Session session = getSession();
        try {
            return session
                    .createQuery("FROM Education e WHERE e.employee.id = :employeeId AND e.employee.deleted = false", Education.class)
                    .setParameter("employeeId", employeeId)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске образования для сотрудника ID: {}", employeeId, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByEmployeeId");
            }
        }
    }

    /**
     * Найти образования даже для удаленных сотрудников (для админских операций)
     */
    public List<Education> findByEmployeeIdIncludingDeleted(@NotNull Long employeeId) {
        if (employeeId == null) {
            log.warn("Попытка найти образование с null ID сотрудника");
            return Collections.emptyList();
        }
        log.debug("Получение образования (включая удаленных) для сотрудника ID: {}", employeeId);
        Session session = getSession();
        try {
            return session
                    .createQuery("FROM Education e WHERE e.employee.id = :employeeId", Education.class)
                    .setParameter("employeeId", employeeId)
                    .getResultList();
        } catch (Exception e) {
            log.error("Ошибка при поиске образования для сотрудника ID: {}", employeeId, e);
            return Collections.emptyList();
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для findByEmployeeIdIncludingDeleted");
            }
        }
    }

    public void save(@NotNull Education education) {
        Objects.requireNonNull(education, "Образование не может быть null");
        Objects.requireNonNull(education.getEmployee(), "Сотрудник в образовании не может быть null");

        // Проверяем, что сотрудник не удален
        if (education.getEmployee().isDeleted()) {
            throw new IllegalStateException("Нельзя добавлять образование удаленному сотруднику");
        }

        log.info("Сохранение образования для сотрудника ID: {}", education.getEmployee().getId());
        Session session = getSession();
        try {
            session.beginTransaction();
            session.persist(education);
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для save");
            }
            log.error("Ошибка при сохранении образования для сотрудника ID: {}",
                    education.getEmployee().getId(), e);
            throw new RuntimeException("Не удалось сохранить образование", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для save");
            }
        }
    }

    public void deleteByEmployeeId(@NotNull Long employeeId) {
        Objects.requireNonNull(employeeId, "ID сотрудника не может быть null");
        log.info("Удаление образования для сотрудника ID: {}", employeeId);
        Session session = getSession();
        try {
            session.beginTransaction();
            session.createMutationQuery("DELETE FROM Education e WHERE e.employee.id = :employeeId")
                    .setParameter("employeeId", employeeId)
                    .executeUpdate();
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для deleteByEmployeeId");
            }
            log.error("Ошибка при удалении образования для сотрудника ID: {}", employeeId, e);
            throw new RuntimeException("Не удалось удалить образование", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для deleteByEmployeeId");
            }
        }
    }

    /**
     * Удалить конкретное образование
     */
    public void delete(@NotNull Education education) {
        Objects.requireNonNull(education, "Образование не может быть null");
        log.info("Удаление образования ID: {}", education.getId());
        Session session = getSession();
        try {
            session.beginTransaction();
            session.remove(education);
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для delete");
            }
            log.error("Ошибка при удалении образования ID: {}", education.getId(), e);
            throw new RuntimeException("Не удалось удалить образование", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для delete");
            }
        }
    }

    /**
     * Обновить образование
     */
    public void update(@NotNull Education education) {
        Objects.requireNonNull(education, "Образование не может быть null");

        // Проверяем, что сотрудник не удален
        if (education.getEmployee().isDeleted()) {
            throw new IllegalStateException("Нельзя обновлять образование удаленного сотрудника");
        }

        log.info("Обновление образования ID: {}", education.getId());
        Session session = getSession();
        try {
            session.beginTransaction();
            session.merge(education);
            session.getTransaction().commit();
        } catch (Exception e) {
            if (session.getTransaction().isActive()) {
                session.getTransaction().rollback();
                log.debug("Транзакция откатана для update");
            }
            log.error("Ошибка при обновлении образования ID: {}", education.getId(), e);
            throw new RuntimeException("Не удалось обновить образование", e);
        } finally {
            if (session.isOpen()) {
                session.close();
                log.debug("Сессия закрыта для update");
            }
        }
    }

    @PreDestroy
    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            log.info("SessionFactory закрыт для EducationRepository");
        }
    }
}