package org.example.repository;

import jakarta.annotation.PreDestroy;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Education;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;


import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * Репозиторий для управления сущностью Education, связанной с сотрудниками.
 */
@Slf4j
@Repository
public class EducationRepository {
    private final SessionFactory sessionFactory;

    /**
     * Конструктор, инициализирующий SessionFactory из hibernate.cfg.xml.
     *
     * @throws RuntimeException если не удалось создать SessionFactory
     */
    public EducationRepository() {
        try {
            this.sessionFactory = new Configuration()
                    .configure() // Загружает hibernate.cfg.xml
                    .buildSessionFactory();
            log.info("SessionFactory успешно создан для EducationRepository");
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
     * Поиск всех записей об образовании для сотрудника по его ID.
     *
     * @param employeeId идентификатор сотрудника
     * @return список записей об образовании или пустой список, если ничего не найдено или employeeId null
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
                    .createQuery("FROM Education e WHERE e.employee.id = :employeeId", Education.class)
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
     * Сохранение записи об образовании.
     *
     * @param education объект образования
     * @throws IllegalArgumentException если education или его employee null
     * @throws RuntimeException если произошла ошибка при сохранении
     */
    public void save(@NotNull Education education) {
        Objects.requireNonNull(education, "Образование не может быть null");
        Objects.requireNonNull(education.getEmployee(), "Сотрудник в образовании не может быть null");
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

    /**
     * Удаление всех записей об образовании для сотрудника по его ID.
     *
     * @param employeeId идентификатор сотрудника
     * @throws IllegalArgumentException если employeeId null
     * @throws RuntimeException если произошла ошибка при удалении
     */
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
     * Закрытие SessionFactory при уничтожении бина.
     */
    @PreDestroy
    public void close() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
            log.info("SessionFactory закрыт для EducationRepository");
        }
    }
}