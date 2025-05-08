CREATE TABLE IF NOT EXISTS Employee
(
    id INT PRIMARY KEY,
    name VARCHAR(30) NOT NULL,
    phoneNumber VARCHAR(20),
    email VARCHAR(100),
    telegram VARCHAR(30),
    resume VARCHAR(100),
    school VARCHAR(100),
    photoPath text,
    skill text,
    skills TEXT
    );

CREATE TABLE IF NOT EXISTS employee_skills
(
    employee_id BIGINT NOT NULL,
    skill VARCHAR(50) NOT NULL,
    PRIMARY KEY (employee_id, skill),
    FOREIGN KEY (employee_id) REFERENCES employee (id)
);
/*CREATE TABLE IF NOT EXISTS Education
(
    id INT PRIMARY KEY,
    year_start INT NOT NULL,
    year_end INT NOT NULL,
    university VARCHAR(100) NOT NULL,
    degree VARCHAR(100) NOT NULL,
    id_employee BIGINT NOT NULL,
    FOREIGN KEY (id_employee) REFERENCES Employee (id)
);*/
INSERT INTO Employee (id, name, phoneNumber, email, telegram, school, photoPath,skill, skills ) VALUES
(1,'Евушко Андрей', +375336980732,'anonim@mail.ru','@ansgoo', '1. СШ №9. 2. УО Малоритский ГПЛ СП.
                        3. н/в IT-Шаг.','/images/2821755862.jpg','Я разработчик. Родом из Бреста в Беларуси.
                        В поисках работы на языке программирования Java.
                        Предпочитаю работать онлайн.','Директор'),
(2,'Вася Пупкин', +375298211966,'test@mail.ru','@test', 'test','/images/2821755862.jpg','test.','Зам');

INSERT INTO skills (id, skill_name, category, proficiency_level, id_employee) VALUES
 (1, 'java','deleop','Expert',1),
 (2, 'Loger','tester','Normal',2);

/*INSERT INTO  education (id, year_start, year_end, university, degree, id_employee) VALUES
(1,'2022','2023',
                 'Lermontov','higher',1),
(2,'2022','2023','Pushin.'
                 ,'higher',1),
(3,'2023','2024','Lermontov','test',2);*/