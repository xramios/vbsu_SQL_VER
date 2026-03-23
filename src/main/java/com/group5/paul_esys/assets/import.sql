CREATE TABLE IF NOT EXISTS enrollment_period
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    school_year varchar(24) not null,
    semester    varchar(24) not null,
    start_date  datetime    not null,
    end_date    datetime    not null,
    updated_at  timestamp default current_timestamp(),
    created_at  timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS users
(
    id       bigint PRIMARY KEY AUTO_INCREMENT,
    email    varchar(255),
    password char(60),
    role     ENUM ('STUDENT', 'REGISTRAR', 'FACULTY')
);

CREATE TABLE IF NOT EXISTS students
(
    student_id     varchar(32) PRIMARY KEY,
    user_id        bigint,
    first_name     varchar(128) NOT NULL,
    last_name      varchar(128) NOT NULL,
    middle_name    varchar(48)  NULL,
    birthdate      date         NOT NULL,
    student_status ENUM ('REGULAR', 'IRREGULAR') DEFAULT 'REGULAR',
    course_id      bigint,
    year_level     int                           default 1,
    created_at     timestamp                     default current_timestamp()
);

CREATE TABLE IF NOT EXISTS subjects
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    subject_name  varchar(32),
    subject_code  varchar(32),
    units         float,
    description   text,
    curriculum_id bigint,
    department_id bigint,
    updated_at    timestamp default current_timestamp(),
    created_at    timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS sections
(
    id           bigint PRIMARY KEY AUTO_INCREMENT,
    section_name varchar(48),
    section_code varchar(48),
    subject_id   bigint,
    capacity     int NOT NULL,
    updated_at   timestamp    default current_timestamp(),
    created_at   timestamp    default current_timestamp()
);

CREATE TABLE IF NOT EXISTS curriculum
(
    id       bigint PRIMARY KEY AUTO_INCREMENT,
    semester varchar(24),
    cur_year date,
    course bigint
);

CREATE TABLE IF NOT EXISTS rooms
(
    id       bigint PRIMARY KEY AUTO_INCREMENT,
    room     varchar(32) NOT NULL,
    capacity int         NOT NULL
);

CREATE TABLE IF NOT EXISTS faculty
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    user_id       bigint,
    first_name    varchar(128),
    last_name     varchar(128),
    department_id bigint,
    updated_at    timestamp default current_timestamp(),
    created_at    timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS schedules
(
    id          bigint PRIMARY KEY AUTO_INCREMENT,
    section_id  bigint,
    room_id     bigint,
    faculty_id  bigint,
    day         ENUM ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN'),
    start_time  time,
    end_time    time,
    school_year varchar(9),
    semester    tinyint,
    updated_at  timestamp default current_timestamp(),
    created_at  timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS enrollments
(
    id           bigint PRIMARY KEY AUTO_INCREMENT,
    student_id   varchar(32),
    school_year  varchar(9),
    semester     tinyint,
    status       ENUM ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'CANCELLED'),
    max_units    float,
    total_units  float,
    submitted_at datetime,
    updated_at   timestamp default current_timestamp(),
    created_at   timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS enrollments_details
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    enrollment_id bigint,
    section_id    bigint,
    subject_id    bigint,
    units         float,
    status        ENUM ('SELECTED', 'DROPPED'),
    created_at    timestamp default current_timestamp(),
    updated_at    timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS student_enrolled_subjects
(
    student_id varchar(32),
    subject_id bigint
);

CREATE TABLE IF NOT EXISTS courses
(
    id            bigint PRIMARY KEY AUTO_INCREMENT,
    course_name   varchar(48),
    description   text,
    department_id bigint,
    updated_at    timestamp default current_timestamp(),
    created_at    timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS prerequisites
(
    id             bigint PRIMARY KEY AUTO_INCREMENT,
    pre_subject_id bigint,
    subject_id     bigint,
    updated_at     timestamp default current_timestamp(),
    created_at     timestamp default current_timestamp()
);

CREATE TABLE IF NOT EXISTS departments
(
    id              bigint PRIMARY KEY AUTO_INCREMENT,
    department_name varchar(48),
    description     text,
    updated_at      timestamp default current_timestamp(),
    created_at      timestamp default current_timestamp()
);

CREATE UNIQUE INDEX student_enrolled_subjects_index_0 ON student_enrolled_subjects (student_id, subject_id);

ALTER TABLE students
    ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE students
    ADD FOREIGN KEY (course_id) REFERENCES courses (id);

ALTER TABLE subjects
    ADD FOREIGN KEY (curriculum_id) REFERENCES curriculum (id);

ALTER TABLE subjects
    ADD FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE sections
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE curriculum
    ADD FOREIGN KEY (course) REFERENCES courses (id);

ALTER TABLE faculty
    ADD FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE faculty
    ADD FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE schedules
    ADD FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE schedules
    ADD FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE schedules
    ADD FOREIGN KEY (faculty_id) REFERENCES faculty (id);

ALTER TABLE enrollments
    ADD FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE enrollments_details
    ADD FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);

ALTER TABLE enrollments_details
    ADD FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE enrollments_details
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE student_enrolled_subjects
    ADD FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE student_enrolled_subjects
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE courses
    ADD FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE prerequisites
    ADD FOREIGN KEY (pre_subject_id) REFERENCES subjects (id);

ALTER TABLE prerequisites
    ADD FOREIGN KEY (subject_id) REFERENCES subjects (id);
