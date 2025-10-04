-- Создание таблицы employees
CREATE TABLE IF NOT EXISTS employees (
                                         id BIGSERIAL PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,
                                         phone_number VARCHAR(255),
                                         email VARCHAR(255) NOT NULL UNIQUE,
                                         telegram VARCHAR(255),
                                         resume VARCHAR(2000),
                                         school VARCHAR(1000),
                                         about VARCHAR(500),
                                         photo_path VARCHAR(255),
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         active BOOLEAN NOT NULL DEFAULT TRUE,
                                         position VARCHAR(255),
                                         department VARCHAR(255),

    -- ПОЛЯ ДЛЯ SOFT DELETE
                                         deleted BOOLEAN NOT NULL DEFAULT FALSE,
                                         deleted_at TIMESTAMP,
                                         deleted_by VARCHAR(255)
);

-- Индексы для таблицы employees
CREATE INDEX IF NOT EXISTS idx_employees_email ON employees (email);
CREATE INDEX IF NOT EXISTS idx_employees_name ON employees (name);
CREATE INDEX IF NOT EXISTS idx_employees_deleted ON employees (deleted);
CREATE INDEX IF NOT EXISTS idx_employees_active_deleted ON employees (active, deleted);
CREATE INDEX IF NOT EXISTS idx_employees_deleted_at ON employees (deleted_at);
CREATE INDEX IF NOT EXISTS idx_employees_position ON employees (position);
CREATE INDEX IF NOT EXISTS idx_employees_department ON employees (department);

-- Таблица навыков сотрудников (используем VARCHAR для хранения enum значений)
CREATE TABLE IF NOT EXISTS employee_skills (
                                               employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                                               skill VARCHAR(255) NOT NULL,
                                               PRIMARY KEY (employee_id, skill)
);

-- Таблица образования
CREATE TABLE IF NOT EXISTS educations (
                                          id BIGSERIAL PRIMARY KEY,
                                          year_start INTEGER NOT NULL CHECK (year_start >= 1900),
                                          year_end INTEGER NOT NULL CHECK (year_end >= 1900),
                                          university VARCHAR(100) NOT NULL,
                                          degree VARCHAR(100) NOT NULL,
                                          employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                                          CHECK (year_end >= year_start)
);

-- Таблица отзывов
CREATE TABLE IF NOT EXISTS reviews (
                                       id BIGSERIAL PRIMARY KEY,
                                       employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                                       rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                                       comment TEXT,
                                       created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Индексы для таблиц
CREATE INDEX IF NOT EXISTS idx_reviews_employee_id ON reviews (employee_id);
CREATE INDEX IF NOT EXISTS idx_educations_employee_id ON educations (employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_skills_employee_id ON employee_skills (employee_id);
CREATE INDEX IF NOT EXISTS idx_employee_skills_skill ON employee_skills (skill);
-- Если меняете на enum хранение, обновите таблицу:
-- Добавление полей для soft delete в существующую таблицу
ALTER TABLE employees
    ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS deleted_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

-- Создание индексов для оптимизации запросов
CREATE INDEX IF NOT EXISTS idx_employees_deleted ON employees (deleted);
CREATE INDEX IF NOT EXISTS idx_employees_active_deleted ON employees (active, deleted);
CREATE INDEX IF NOT EXISTS idx_employees_deleted_at ON employees (deleted_at);

-- Создание функции для триггера
CREATE OR REPLACE FUNCTION update_updated_at_column()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Создание триггера
CREATE OR REPLACE TRIGGER update_employees_updated_at
    BEFORE UPDATE ON employees
    FOR EACH ROW
EXECUTE FUNCTION update_updated_at_column();

-- Представление для активных сотрудников
CREATE OR REPLACE VIEW active_employees_view AS
SELECT
    e.id,
    e.name,
    e.email,
    e.phone_number,
    e.telegram,
    e.position,
    e.department,
    e.photo_path,
    e.created_at,
    e.updated_at,
    e.active,
    (SELECT COUNT(*) FROM reviews r WHERE r.employee_id = e.id) as review_count,
    (SELECT AVG(rating) FROM reviews r WHERE r.employee_id = e.id) as avg_rating
FROM employees e
WHERE e.deleted = false AND e.active = true;

-- Представление для удаленных сотрудников
CREATE OR REPLACE VIEW deleted_employees_view AS
SELECT
    e.id,
    e.name,
    e.email,
    e.position,
    e.department,
    e.deleted_at,
    e.deleted_by,
    e.created_at
FROM employees e
WHERE e.deleted = true;

-- Представление для статистики по отделам
CREATE OR REPLACE VIEW department_stats_view AS
SELECT
    department,
    COUNT(*) as total_employees,
    COUNT(*) FILTER (WHERE deleted = false AND active = true) as active_employees,
    COUNT(*) FILTER (WHERE deleted = true) as deleted_employees,
    COUNT(*) FILTER (WHERE deleted = false AND active = false) as inactive_employees
FROM employees
GROUP BY department
ORDER BY department;

-- Представление для навыков сотрудников
CREATE OR REPLACE VIEW employee_skills_view AS
SELECT
    e.id as employee_id,
    e.name as employee_name,
    e.position,
    e.department,
    es.skill,
    e.deleted
FROM employees e
         JOIN employee_skills es ON e.id = es.employee_id
WHERE e.deleted = false;

-- Очистка существующих данных (опционально)
-- DELETE FROM reviews;
-- DELETE FROM educations;
-- DELETE FROM employee_skills;
-- DELETE FROM employees;

-- Вставка тестовых данных сотрудников
INSERT INTO employees (name, phone_number, email, telegram, resume, school, about, photo_path, position, department, deleted, deleted_at) VALUES
                                                                                                                                              ('Евушко Андрей', '+375336980732', 'anonim@mail.ru', '@ansgoo',
                                                                                                                                               'Я разработчик. Родом из Бреста в Беларуси. В поисках работы на языке программирования Java. Предпочитаю работать онлайн.',
                                                                                                                                               '1. СШ №9. 2. УО Малоритский ГПЛ СП. 3. н/в IT-Шаг.',
                                                                                                                                               'Разработчик Java, увлечен созданием масштабируемых приложений.',
                                                                                                                                               '/images/eab2e77f92de15a95ebf828c08fe5290.jpg', 'Java Developer', 'IT', false, null),

                                                                                                                                              ('Вася Пупкин', '+375298211966', 'test@mail.ru', '@test',
                                                                                                                                               'Тестовое резюме.',
                                                                                                                                               'Тестовая школа',
                                                                                                                                               'Тестировщик с опытом автоматизации.',
                                                                                                                                               '/images/2821755862.jpg', 'QA Engineer', 'Testing', false, null),

                                                                                                                                              ('Иван Удаленный', '+375291234567', 'deleted@mail.ru', '@deleted',
                                                                                                                                               'Удаленный сотрудник.',
                                                                                                                                               'Университет удаленных наук',
                                                                                                                                               'Этот сотрудник был удален.',
                                                                                                                                               '/images/default.jpg', 'Former Employee', 'Archived', true, CURRENT_TIMESTAMP),

                                                                                                                                              ('Мария Разработчик', '+375297654321', 'maria@mail.ru', '@maria',
                                                                                                                                               'Full-stack разработчик с опытом работы.',
                                                                                                                                               'БГУИР',
                                                                                                                                               'Опыт в веб-разработке и мобильных приложениях.',
                                                                                                                                               '/images/default.jpg', 'Full Stack Developer', 'IT', false, null),

                                                                                                                                              ('Петр Менеджер', '+375296665544', 'petr@mail.ru', '@petr',
                                                                                                                                               'Менеджер проектов.',
                                                                                                                                               'БГЭУ',
                                                                                                                                               'Управление IT проектами.',
                                                                                                                                               '/images/default.jpg', 'Project Manager', 'Management', false, null)
ON CONFLICT (email) DO NOTHING;

-- Вставка навыков (используем значения из вашего enum)
INSERT INTO employee_skills (employee_id, skill) VALUES
                                                     ((SELECT id FROM employees WHERE email = 'anonim@mail.ru'), 'JAVA'),
                                                     ((SELECT id FROM employees WHERE email = 'anonim@mail.ru'), 'SPRING'),
                                                     ((SELECT id FROM employees WHERE email = 'anonim@mail.ru'), 'HIBERNATE'),
                                                     ((SELECT id FROM employees WHERE email = 'anonim@mail.ru'), 'SQL'),
                                                     ((SELECT id FROM employees WHERE email = 'anonim@mail.ru'), 'DOCKER'),

                                                     ((SELECT id FROM employees WHERE email = 'test@mail.ru'), 'JAVA'),
                                                     ((SELECT id FROM employees WHERE email = 'test@mail.ru'), 'SELENIUM'),
                                                     ((SELECT id FROM employees WHERE email = 'test@mail.ru'), 'JUNIT'),
                                                     ((SELECT id FROM employees WHERE email = 'test@mail.ru'), 'PYTEST'),

                                                     ((SELECT id FROM employees WHERE email = 'deleted@mail.ru'), 'JAVA'),
                                                     ((SELECT id FROM employees WHERE email = 'deleted@mail.ru'), 'SPRING'),

                                                     ((SELECT id FROM employees WHERE email = 'maria@mail.ru'), 'JAVA'),
                                                     ((SELECT id FROM employees WHERE email = 'maria@mail.ru'), 'SPRING'),
                                                     ((SELECT id FROM employees WHERE email = 'maria@mail.ru'), 'REACT'),
                                                     ((SELECT id FROM employees WHERE email = 'maria@mail.ru'), 'POSTGRESQL'),

                                                     ((SELECT id FROM employees WHERE email = 'petr@mail.ru'), 'GIT'),
                                                     ((SELECT id FROM employees WHERE email = 'petr@mail.ru'), 'JENKINS')
ON CONFLICT (employee_id, skill) DO NOTHING;

-- Вставка образования
INSERT INTO educations (year_start, year_end, university, degree, employee_id) VALUES
                                                                                   (2022, 2023, 'Университет им. Лермонтова', 'Бакалавр', (SELECT id FROM employees WHERE email = 'anonim@mail.ru')),
                                                                                   (2022, 2023, 'Университет им. Пушкина', 'Магистр', (SELECT id FROM employees WHERE email = 'anonim@mail.ru')),

                                                                                   (2023, 2024, 'Университет им. Лермонтова', 'Специалист', (SELECT id FROM employees WHERE email = 'test@mail.ru')),

                                                                                   (2020, 2021, 'Университет Архива', 'Доктор наук', (SELECT id FROM employees WHERE email = 'deleted@mail.ru')),

                                                                                   (2018, 2022, 'БГУИР', 'Бакалавр', (SELECT id FROM employees WHERE email = 'maria@mail.ru')),
                                                                                   (2022, 2023, 'БГУИР', 'Магистр', (SELECT id FROM employees WHERE email = 'maria@mail.ru')),

                                                                                   (2015, 2019, 'БГЭУ', 'Бакалавр', (SELECT id FROM employees WHERE email = 'petr@mail.ru'))
ON CONFLICT (id) DO NOTHING;

-- Вставка отзывов
INSERT INTO reviews (employee_id, rating, comment, created_at) VALUES
                                                                   ((SELECT id FROM employees WHERE email = 'anonim@mail.ru'), 5, 'Отличный разработчик! Очень ответственный и квалифицированный.', CURRENT_TIMESTAMP),
                                                                   ((SELECT id FROM employees WHERE email = 'anonim@mail.ru'), 4, 'Хорошо справляется с задачами, иногда нужно больше времени.', CURRENT_TIMESTAMP - INTERVAL '5 days'),

                                                                   ((SELECT id FROM employees WHERE email = 'test@mail.ru'), 4, 'Хороший тестировщик, находит сложные баги.', CURRENT_TIMESTAMP),
                                                                   ((SELECT id FROM employees WHERE email = 'test@mail.ru'), 3, 'Нужно улучшить документацию тестов.', CURRENT_TIMESTAMP - INTERVAL '10 days'),

                                                                   ((SELECT id FROM employees WHERE email = 'deleted@mail.ru'), 3, 'Старый отзыв на удаленного сотрудника.', CURRENT_TIMESTAMP - INTERVAL '30 days'),

                                                                   ((SELECT id FROM employees WHERE email = 'maria@mail.ru'), 5, 'Отличный full-stack разработчик!', CURRENT_TIMESTAMP),

                                                                   ((SELECT id FROM employees WHERE email = 'petr@mail.ru'), 4, 'Хороший менеджер проектов.', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO NOTHING;

-- Все активные сотрудники
SELECT * FROM active_employees_view;

-- Все удаленные сотрудники
SELECT * FROM deleted_employees_view;

-- Статистика по отделам
SELECT * FROM department_stats_view;

-- Навыки активных сотрудников
SELECT * FROM employee_skills_view;

-- Поиск сотрудников по навыку
SELECT e.name, e.position, e.department
FROM employees e
         JOIN employee_skills es ON e.id = es.employee_id
WHERE es.skill = 'JAVA'
  AND e.deleted = false
  AND e.active = true;

-- Восстановление сотрудника
UPDATE employees
SET deleted = false, deleted_at = NULL, deleted_by = NULL, active = true
WHERE id = 3;

-- Мягкое удаление сотрудника
UPDATE employees
SET deleted = true, deleted_at = CURRENT_TIMESTAMP, deleted_by = 'admin'
WHERE id = 2;