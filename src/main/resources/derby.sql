CREATE TABLE enrollment_period
(
    id          bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    school_year varchar(24) not null,
    semester    varchar(24) not null,
    start_date  TIMESTAMP   not null,
    end_date    TIMESTAMP   not null,
    updated_at  timestamp default current_timestamp,
    created_at  timestamp default current_timestamp
);

CREATE TABLE users
(
    id       bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email    varchar(255),
    password char(60),
    role     varchar(20) CHECK (role IN ('STUDENT', 'REGISTRAR', 'FACULTY'))
);

CREATE TABLE students
(
    student_id     varchar(32) PRIMARY KEY,
    user_id        bigint,
    first_name     varchar(128) NOT NULL,
    last_name      varchar(128) NOT NULL,
    middle_name    varchar(48),
    birthdate      date         NOT NULL,
    student_status varchar(20) DEFAULT 'REGULAR' CHECK (student_status IN ('REGULAR', 'IRREGULAR')),
    course_id      bigint,
    year_level     int         default 1,
    created_at     timestamp   default current_timestamp
);

CREATE TABLE subjects
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    subject_name  varchar(32),
    subject_code  varchar(32),
    units         float,
    description   clob,
    curriculum_id bigint,
    department_id bigint,
    updated_at    timestamp default current_timestamp,
    created_at    timestamp default current_timestamp
);

CREATE TABLE sections
(
    id           bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_name varchar(48),
    section_code varchar(48),
    subject_id   bigint,
    capacity     int    NOT NULL,
    updated_at   timestamp default current_timestamp,
    created_at   timestamp default current_timestamp
);

CREATE TABLE curriculum
(
    id       bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    cur_year date,
    course   bigint
);

CREATE TABLE semester
(
    id            bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    curriculum_id bigint      NOT NULL REFERENCES curriculum (id),
    semester      varchar(24) NOT NULL,
    created_at    timestamp default current_timestamp,
    updated_at    timestamp default current_timestamp
);

CREATE TABLE semester_subjects
(
    semester_id bigint NOT NULL REFERENCES semester (id),
    subject_id  bigint NOT NULL REFERENCES subjects (id),
    year_level  int    NOT NULL, -- Ito yung year na dapat kunin ng student
    created_at  timestamp default current_timestamp,
    updated_at  timestamp default current_timestamp
);

-- Need natin ng table na ito para ma-track natin kung anong subjects ang enrolled ng student,
-- para ma-check natin sa prerequisites kung eligible ba siya mag-enroll sa subject na gusto niya
-- Tsaka dapat matake niya lahat pero pwede na per year, onti lang yung subject na ittake niya.
CREATE TABLE student_subjects
(
    student_id          varchar(32)                                                        NOT NULL REFERENCES students (student_id),
    semester_subject_id bigint                                                             NOT NULL REFERENCES semester_subjects (semester_id),
    status              varchar(20) CHECK (status IN ('ENROLLED', 'COMPLETED', 'DROPPED')) NOT NULL DEFAULT 'ENROLLED',
    created_at          timestamp                                                                   default current_timestamp,
    updated_at          timestamp                                                                   default current_timestamp
);

CREATE TABLE rooms
(
    id       bigint      NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    room     varchar(32) NOT NULL,
    capacity int         NOT NULL
);

CREATE TABLE faculty
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    user_id       bigint,
    first_name    varchar(128),
    last_name     varchar(128),
    department_id bigint,
    updated_at    timestamp default current_timestamp,
    created_at    timestamp default current_timestamp
);

CREATE TABLE schedules
(
    id          bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    section_id  bigint,
    room_id     bigint,
    faculty_id  bigint,
    day         varchar(3) CHECK (day IN ('MON', 'TUE', 'WED', 'THU', 'FRI', 'SAT', 'SUN')),
    start_time  time,
    end_time    time,
    school_year varchar(9),
    semester    SMALLINT,
    updated_at  timestamp default current_timestamp,
    created_at  timestamp default current_timestamp
);

CREATE TABLE enrollments
(
    id           bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    student_id   varchar(32),
    school_year  varchar(9),
    semester     SMALLINT,
    status       varchar(20) CHECK (status IN ('DRAFT', 'SUBMITTED', 'APPROVED', 'ENROLLED', 'CANCELLED')),
    max_units    float,
    total_units  float,
    submitted_at TIMESTAMP,
    updated_at   timestamp default current_timestamp,
    created_at   timestamp default current_timestamp
);

CREATE TABLE enrollments_details
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    enrollment_id bigint,
    section_id    bigint,
    subject_id    bigint,
    units         float,
    status        varchar(20) CHECK (status IN ('SELECTED', 'DROPPED')),
    created_at    timestamp default current_timestamp,
    updated_at    timestamp default current_timestamp
);

CREATE TABLE student_enrolled_subjects
(
    student_id varchar(32),
    subject_id bigint
);

CREATE TABLE courses
(
    id            bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    course_name   varchar(48),
    description   clob,
    department_id bigint,
    updated_at    timestamp default current_timestamp,
    created_at    timestamp default current_timestamp
);

CREATE TABLE prerequisites
(
    id             bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pre_subject_id bigint,
    subject_id     bigint,
    updated_at     timestamp default current_timestamp,
    created_at     timestamp default current_timestamp
);

CREATE TABLE departments
(
    id              bigint NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    department_name varchar(48),
    description     clob,
    updated_at      timestamp default current_timestamp,
    created_at      timestamp default current_timestamp
);

CREATE UNIQUE INDEX student_enrolled_subjects_index_0 ON student_enrolled_subjects (student_id, subject_id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE students
    ADD CONSTRAINT fk_students_course FOREIGN KEY (course_id) REFERENCES courses (id);

ALTER TABLE subjects
    ADD CONSTRAINT fk_subjects_curriculum FOREIGN KEY (curriculum_id) REFERENCES curriculum (id);

ALTER TABLE subjects
    ADD CONSTRAINT fk_subjects_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE sections
    ADD CONSTRAINT fk_sections_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE curriculum
    ADD CONSTRAINT fk_curriculum_course FOREIGN KEY (course) REFERENCES courses (id);

ALTER TABLE faculty
    ADD CONSTRAINT fk_faculty_user FOREIGN KEY (user_id) REFERENCES users (id);

ALTER TABLE faculty
    ADD CONSTRAINT fk_faculty_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_section FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_room FOREIGN KEY (room_id) REFERENCES rooms (id);

ALTER TABLE schedules
    ADD CONSTRAINT fk_schedules_faculty FOREIGN KEY (faculty_id) REFERENCES faculty (id);

ALTER TABLE enrollments
    ADD CONSTRAINT fk_enrollments_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_enrollment FOREIGN KEY (enrollment_id) REFERENCES enrollments (id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_section FOREIGN KEY (section_id) REFERENCES sections (id);

ALTER TABLE enrollments_details
    ADD CONSTRAINT fk_ed_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_student FOREIGN KEY (student_id) REFERENCES students (student_id);

ALTER TABLE student_enrolled_subjects
    ADD CONSTRAINT fk_ses_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);

ALTER TABLE courses
    ADD CONSTRAINT fk_courses_dept FOREIGN KEY (department_id) REFERENCES departments (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_presubject FOREIGN KEY (pre_subject_id) REFERENCES subjects (id);

ALTER TABLE prerequisites
    ADD CONSTRAINT fk_prereq_subject FOREIGN KEY (subject_id) REFERENCES subjects (id);