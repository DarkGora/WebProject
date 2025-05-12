-- Создаем таблицу Employee
CREATE TABLE employee (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          phone_number VARCHAR(20), -- Изменено на snake_case для консистентности
                          email VARCHAR(100) NOT NULL UNIQUE, -- Добавлено NOT NULL
                          telegram VARCHAR(50), -- Увеличена длина до 50
                          resume TEXT,
                          school TEXT,
                          photo_path TEXT, -- Изменено на snake_case
                          category VARCHAR(50), -- Изменено на VARCHAR(50) вместо TEXT
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP -- Добавлено NOT NULL
);

-- Создаем таблицу для навыков
CREATE TABLE employee_skills (
                                 employee_id BIGINT NOT NULL,
                                 skill VARCHAR(50) NOT NULL,
                                 PRIMARY KEY (employee_id, skill),
                                 FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE,
                                 CONSTRAINT valid_skill CHECK (skill IN (
                                                                         'JAVA', 'SPRING', 'SPRING_BOOT', 'SQL', 'REACT', 'HIBERNATE',
                                                                         'DEVELOPMENT', 'EXPERT', 'TESTER', 'NORMAL'
                                     )) -- Добавлены недостающие навыки из Java-кода
);
-- Создаем таблицу Education (раскомментировано и исправлено)
CREATE TABLE IF NOT EXISTS education (
                                         id BIGSERIAL PRIMARY KEY, -- Изменено на BIGSERIAL для консистентности
                                         year_start INT NOT NULL CHECK (year_start > 1900), -- Добавлена проверка
                                         year_end INT NOT NULL CHECK (year_end >= year_start), -- Добавлена проверка
                                         university VARCHAR(100) NOT NULL,
                                         degree VARCHAR(100) NOT NULL,
                                         employee_id BIGINT NOT NULL, -- Изменено на snake_case
                                         FOREIGN KEY (employee_id) REFERENCES employee(id) ON DELETE CASCADE
);

-- Вставляем сотрудников
INSERT INTO employee (
    name, phone_number, email, telegram, resume, school, photo_path, category
) VALUES (
             'Евушко Андрей',
             '+375336980732',
             'anonim@mail.ru',
             '@ansgoo',
             '1. СШ №9. 2. УО Малоритский ГПЛ СП. 3. н/в IT-Шаг.',
             'IT-Шаг',
             '/images/2821755862.jpg',
             'Java Developer' -- Добавлено значение для category
         ), (
             'Вася Пупкин',
             '+375298211966',
             'test@mail.ru',
             '@test',
             'Опыт работы: 5 лет Full-stack разработчиком',
             'БГУ',
             '/images/employee2.jpg',
             'Full-stack Developer' -- Добавлено значение для category
         );

-- Вставляем навыки для сотрудников
INSERT INTO employee_skills (employee_id, skill) VALUES
                                                     (1, 'JAVA'),
                                                     (1, 'SPRING'),
                                                     (1, 'SQL'),
                                                     (2, 'JAVA'),
                                                     (2, 'SPRING_BOOT'),
                                                     (2, 'REACT');

-- Вставляем данные об образовании
INSERT INTO education (year_start, year_end, university, degree, employee_id) VALUES
                                                                                  (2022, 2023, 'Лермонтов', 'Высшее', 1),
                                                                                  (2022, 2023, 'Пушкин', 'Высшее', 1),
                                                                                  (2023, 2024, 'Лермонтов', 'Техническое', 2);