CREATE SCHEMA if not exists s;

-- пользователь
CREATE TABLE s.users (
                         user_id SERIAL PRIMARY KEY,
                         telegram_id BIGINT UNIQUE,
                         username VARCHAR(30) UNIQUE,
                         full_name VARCHAR(30) NOT NULL,
                         registration_date DATE NOT NULL,
                         profession_id INT
);
--статус конференции
CREATE TABLE s.conference_status (
                                     status_id SERIAL PRIMARY KEY,
                                     status_name VARCHAR(30) NOT NULL UNIQUE,
                                     description TEXT
);

CREATE TABLE s.profession (
                              profession_id SERIAL PRIMARY KEY,
                              profession_name VARCHAR(100) NOT NULL UNIQUE
);


ALTER TABLE s.users ADD CONSTRAINT fk_profession
    FOREIGN KEY (profession_id) REFERENCES s.profession(profession_id);

CREATE TABLE s.conference_theme (
                                    theme_id SERIAL PRIMARY KEY,
                                    user_id INT NOT NULL REFERENCES s.users(user_id),
                                    theme_name VARCHAR(60) NOT NULL,
                                    description TEXT,
                                    is_active BOOLEAN DEFAULT TRUE,
                                    vote_theme int not null default 0
);

CREATE TABLE s.conference (
                              conference_id SERIAL PRIMARY KEY,                   -- Уникальный идентификатор конференции (автоинкремент)
                              theme_id INT NOT NULL REFERENCES s.conference_theme(theme_id),  -- ID темы конференции (внешний ключ)
                              status_id INT NOT NULL REFERENCES s.conference_status(status_id), -- ID статуса конференции (внешний ключ)
                              event_date TIMESTAMP NOT NULL,                      -- Дата и время проведения конференции
                              meeting_link VARCHAR(60)                            -- Ссылка для подключения к конференции (макс. 60 символов)
);
-- участники, штрафы
CREATE TABLE s.conference_participation (
                                            participation_id SERIAL PRIMARY KEY,
                                            user_id INT NOT NULL REFERENCES s.users(user_id), -- Идентификатор пользователя
                                            conference_id INT NOT NULL REFERENCES s.conference(conference_id),  -- Идентификатор конференции
                                            is_attended BOOLEAN DEFAULT FALSE, -- Флаг "присутствовал ли участник"
                                            absence_reason TEXT,-- Причина отсутствия (текстовое поле)
                                            absence_submission_date TIMESTAMP,  -- Дата и время подачи причины отсутствия
                                            fine_amount DECIMAL(10,2), --штрафы
                                            is_fine_paid BOOLEAN DEFAULT FALSE, -- Флаг "оплачен ли штраф" (по умолчанию: нет)
                                            fine_payment_date DATE, -- Дата оплаты штрафа
                                            points INT DEFAULT 0, --баллы
                                            UNIQUE (user_id, conference_id)
);

CREATE TABLE s.rule_types (
                              type_id SERIAL PRIMARY KEY,          -- Уникальный ID типа правила
                              type_name TEXT NOT NULL UNIQUE,      -- Название типа (уникальное)
                              description TEXT                    -- Описание типа (необязательное)
);
CREATE TABLE s.rules (
                         rule_id SERIAL PRIMARY KEY,          -- Уникальный ID правила
                         current_version_id INT,              -- ID текущей версии правила
                         date_offers DATE NOT NULL,           -- Дата предложения правила
                         user_id INT NOT NULL REFERENCES s.users(user_id), -- ID пользователя, создавшего правило
                         type_id INT REFERENCES s.rule_types(type_id),     -- ID типа правила
                         implementation_date DATE,            -- Дата внедрения правила
                         description TEXT NOT NULL            -- Описание правила
);

CREATE TABLE s.rule_versions (
                                 version_id SERIAL PRIMARY KEY,
                                 rule_id INT NOT NULL REFERENCES s.rules(rule_id), -- ID правила (внешний ключ)
                                 version_number INT NOT NULL, -- Номер версии
                                 description TEXT NOT NULL,-- Описание изменений
                                 application_date DATE NOT NULL,-- Дата вступления в силу
                                 implementation_date DATE,-- Дата фактического внедрения
                                 status BOOLEAN NOT NULL DEFAULT FALSE,-- Статус (активна/неактивна)
                                 modified_by INT NOT NULL REFERENCES s.users(user_id),-- Кто изменил (ID пользователя)
                                 modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,  -- Дата и время изменения
                                 UNIQUE (rule_id, version_number)
);

CREATE TABLE s.rule_votes (
                              vote_id SERIAL PRIMARY KEY,
                              rule_id INT NOT NULL REFERENCES s.rules(rule_id),
                              user_id INT NOT NULL REFERENCES s.users(user_id),
                              vote_value INT NOT NULL CHECK (vote_value IN (1, -1)),
                              vote_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE (rule_id, user_id)
);
--График конференции
CREATE TABLE s.schedule (
                            schedule_id SERIAL PRIMARY KEY,
                            conference_id INT NOT NULL REFERENCES s.conference(conference_id),
                            participant_id INT NOT NULL REFERENCES s.conference_participation(participation_id),
                            slot_time TIMESTAMP NOT NULL,
                            UNIQUE (conference_id, slot_time)
);

CREATE TABLE s.vote (
                        vote_id SERIAL PRIMARY KEY,
                        conference_id INT NOT NULL,
                        voter_id INT NOT NULL,
                        best_speaker_id INT NOT NULL,
                        worst_speaker_id INT NOT NULL,
                        best_moderator_id INT NOT NULL,
                        worst_moderator_id INT NOT NULL,
                        vote_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        FOREIGN KEY (conference_id) REFERENCES s.conference(conference_id),
                        FOREIGN KEY (voter_id) REFERENCES s.users(user_id),
                        FOREIGN KEY (best_speaker_id) REFERENCES s.users(user_id),
                        FOREIGN KEY (worst_speaker_id) REFERENCES s.users(user_id),
                        FOREIGN KEY (best_moderator_id) REFERENCES s.users(user_id),
                        FOREIGN KEY (worst_moderator_id) REFERENCES s.users(user_id),
                        CHECK (voter_id != best_speaker_id),
                        CHECK (voter_id != worst_speaker_id),
                        CHECK (voter_id != best_moderator_id),
                        CHECK (voter_id != worst_moderator_id),
                        UNIQUE (conference_id, voter_id)
);


INSERT INTO s.profession (profession_id, profession_name)
VALUES
    (1, 'Директор'),
    (2, 'Программист'),
    (3, 'Дизайнер'),
    (4, 'Менеджер');

INSERT INTO s.users (user_id, telegram_id, username, full_name, registration_date, profession_id)
VALUES
    (1, 12123, '@Vasya', 'Василий Петров', '2023-12-12', 1),
    (2, 12124, '@Masha', 'Мария Иванова', '2024-01-23', 2),
    (3, 12125, '@Vera', 'Вера Сидорова', '2023-11-15', 3),
    (4, 12126, '@Dima', 'Дмитрий Смирнов', '2023-10-20', 4),
    (5, 12196, '@Danik', 'Danniil Kuk', '2023-10-20', 4),
    (6, 12197, '@Alex', 'Александр Волков', '2024-02-15', 2),  -- Новый пользователь: программист
    (7, 12198, '@Olga', 'Ольга Козлова', '2024-03-01', 3);

INSERT INTO s.conference_status (status_id, status_name, description)
VALUES
    (1, 'Активна', 'сегодня'),
    (2, 'Завершена', 'прошла'),
    (3, 'Запланирована', 'в будущем');

INSERT INTO s.conference_theme (theme_id, user_id, theme_name, description, is_active,vote_theme)
VALUES
    (1, 1, 'ИТ', 'Новые технологии', TRUE, 2),
    (2, 2, 'ИТ', 'Новые технологии - продолжение', TRUE, 2),
    (3, 3, 'Будущее', 'Тренды в IT', TRUE, 1),
    (4, 4, 'Будущее', 'Бизнес-тренды', TRUE, 1),
    (5, 3, 'Управление проектами', 'Эффективное управление', FALSE, 4);

INSERT INTO s.conference (conference_id, theme_id, status_id, event_date, meeting_link)
VALUES
    (1, 1, 1, '2024-06-15 14:00:00', 'www.loki1'),
    (2, 2, 3, '2024-07-20 15:30:00', 'www.loki2'),  -- Исправлено: убрано дублирование ID
    (3, 3, 2, '2024-05-10 10:00:00', 'www.loki3');

INSERT INTO s.conference_participation (participation_id, user_id, conference_id, is_attended, absence_reason, fine_amount,points)
VALUES
    (1, 1, 1, TRUE, NULL, NULL,0),
    (2, 2, 1, FALSE, 'Болезнь', 500.00,+3),
    (3, 3, 2, TRUE, NULL, NULL,1),
    (4, 4, 3, FALSE, 'Командировка', 1000.00,99);

INSERT INTO s.schedule (schedule_id, conference_id, participant_id, slot_time)
VALUES
    (1, 1, 1, '2024-06-15 14:30:00'),
    (2, 1, 2, '2024-06-15 15:00:00'),
    (3, 2, 3, '2024-07-20 16:00:00');

INSERT INTO s.rule_types (type_id, type_name, description)
VALUES
    (1, 'Организационные', 'проведения'),
    (2, 'Поведенческие', 'Этикет'),
    (3, 'Технические', 'Требования');

INSERT INTO s.rules (rule_id, date_offers, user_id, type_id, implementation_date, description)
VALUES
    (1, '2024-01-10', 1, 1, '2024-02-01', 'Пунктуальность'),
    (2, '2024-03-15', 2, 2, NULL, 'Дресс-код'),
    (3, '2024-05-20', 3, 3, '2024-06-01', 'Формат');

INSERT INTO s.rule_versions (version_id, rule_id, version_number, description, application_date, implementation_date, status, modified_by, modified_at)
VALUES
    (1, 1, 1, 'о пунктуальности', '2024-01-10', '2024-02-01', TRUE, 1, '2024-01-10 12:00:00'),
    (2, 2, 1, 'дресс-код', '2024-03-15', NULL, FALSE, 2, '2024-03-15 14:30:00'),
    (3, 3, 1, 'требования', '2024-05-20', '2024-06-01', TRUE, 3, '2024-05-20 10:15:00');

UPDATE s.rules SET current_version_id = 1 WHERE rule_id = 1;
UPDATE s.rules SET current_version_id = 2 WHERE rule_id = 2;
UPDATE s.rules SET current_version_id = 3 WHERE rule_id = 3;

INSERT INTO s.rule_votes (vote_id, rule_id, user_id, vote_value, vote_date)
VALUES
    (1, 1, 1, 1, '2024-01-11 09:00:00'),
    (2, 1, 2, 1, '2024-01-11 10:30:00'),
    (3, 2, 3, -1, '2024-03-16 11:45:00'),
    (4, 3, 4, 1, '2024-05-21 15:20:00');

INSERT INTO s.vote (
    vote_id,
    conference_id,
    voter_id,
    best_speaker_id,
    worst_speaker_id,
    best_moderator_id,
    worst_moderator_id,
    vote_date
) VALUES
      (1, 1, 1, 2, 3, 4, 5, '2024-06-15 16:00:00'),  -- Исправлено: worst_moderator_id изменен с 2 на 5
      (2, 1, 2, 1, 3, 4, 5, '2024-06-15 16:30:00'),  -- Исправлено: worst_moderator_id изменен с 1 на 5
      (3, 2, 3, 4, 1, 2, 5, '2024-07-20 17:00:00');  -- Исправлено: worst_moderator_id изменен с 4 на 5
