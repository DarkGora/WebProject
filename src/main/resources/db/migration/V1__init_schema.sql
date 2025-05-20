-- Create tables
CREATE TABLE IF NOT EXISTS employees (
                                         id BIGSERIAL PRIMARY KEY,
                                         name VARCHAR(255) NOT NULL,
                                         phone_number VARCHAR(255),
                                         email VARCHAR(255) NOT NULL UNIQUE,
                                         telegram VARCHAR(255),
                                         resume TEXT,
                                         school TEXT,
                                         about TEXT,
                                         photo_path VARCHAR(255),
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_employees_email ON employees (email);

CREATE TABLE IF NOT EXISTS employee_skills (
                                               employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                                               skill VARCHAR(255) NOT NULL,
                                               PRIMARY KEY (employee_id, skill)
);

CREATE TABLE IF NOT EXISTS educations (
                                          id BIGSERIAL PRIMARY KEY,
                                          year_start INTEGER NOT NULL CHECK (year_start >= 1900),
                                          year_end INTEGER NOT NULL CHECK (year_end >= 1900),
                                          university VARCHAR(100) NOT NULL,
                                          degree VARCHAR(100) NOT NULL,
                                          employee_id BIGINT NOT NULL REFERENCES employees(id) ON DELETE CASCADE,
                                          CHECK (year_end >= year_start)
);

CREATE INDEX IF NOT EXISTS idx_educations_employee_id ON educations (employee_id);

-- Insert initial data
INSERT INTO employees (name, phone_number, email, telegram, resume, school, about, photo_path)
VALUES
    ('Евушко Андрей', '+375336980732', 'anonim@mail.ru', '@ansgoo',
     'Я разработчик. Родом из Бреста в Беларуси. В поисках работы на языке программирования Java. Предпочитаю работать онлайн.',
     '1. СШ №9. 2. УО Малоритский ГПЛ СП. 3. н/в IT-Шаг.',
     'Разработчик Java, увлечен созданием масштабируемых приложений.',
     '/images/2821755862.jpg'),
    ('Вася Пупкин', '+375298211966', 'test@mail.ru', '@test',
     'Тестовое резюме.',
     'Тестовая школа',
     'Тестировщик с опытом автоматизации.',
     '/images/2821755862.jpg')
ON CONFLICT (email) DO NOTHING;

INSERT INTO employee_skills (employee_id, skill)
VALUES
    (1, 'JAVA'),
    (1, 'SPRING'),
    (2, 'TESTING')
ON CONFLICT (employee_id, skill) DO NOTHING;

INSERT INTO educations (year_start, year_end, university, degree, employee_id)
VALUES
    (2022, 2023, 'Университет им. Лермонтова', 'Бакалавр', 1),
    (2022, 2023, 'Университет им. Пушкина', 'Магистр', 1),
    (2023, 2024, 'Университет им. Лермонтова', 'Специалист', 2)
ON CONFLICT (id) DO NOTHING;