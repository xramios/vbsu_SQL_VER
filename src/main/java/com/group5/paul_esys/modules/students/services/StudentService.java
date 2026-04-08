package com.group5.paul_esys.modules.students.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.model.StudentStatus;
import com.group5.paul_esys.modules.students.utils.StudentMapper;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.time.Year;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StudentService {

    private static final StudentService INSTANCE = new StudentService();
    private static final Logger logger = LoggerFactory.getLogger(
        StudentService.class
    );

    private StudentService() {}

    public static StudentService getInstance() {
        return INSTANCE;
    }

    public Optional<Student> get(String studentId) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getRow() > 0) {
                        return Optional.of(StudentMapper.mapToStudent(rs));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Student> insert(Student student) {
        String sql =
            "INSERT INTO students (student_id, user_id, first_name, last_name, middle_name, birthdate, student_status, course_id, year_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, student.getStudentId());
            stmt.setLong(2, student.getUserId());
            stmt.setString(3, student.getFirstName());
            stmt.setString(4, student.getLastName());
            stmt.setString(5, student.getMiddleName());
            stmt.setDate(
                6,
                new java.sql.Date(student.getBirthdate().getTime())
            );
            stmt.setString(7, student.getStudentStatus().name());
            stmt.setLong(8, student.getCourseId());
            stmt.setLong(9, student.getYearLevel());

            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                logger.info("A new student was inserted successfully!");
                return Optional.of(student);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<Student> registerStudent(String email, String plainPassword, Student student) {
        String userSql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";
        String studentSql =
            "INSERT INTO students (student_id, user_id, first_name, last_name, middle_name, birthdate, student_status, course_id, year_level) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        String studentId = student.getStudentId();
        if (studentId == null || studentId.isBlank()) {
            studentId = generateStudentId();
            student.setStudentId(studentId);
        }

        String hashedPassword = BCrypt
            .withDefaults()
            .hashToString(12, plainPassword.toCharArray());

        try (Connection conn = ConnectionService.getConnection()) {
            conn.setAutoCommit(false);

            try (
                PreparedStatement userStmt = conn.prepareStatement(
                    userSql,
                    Statement.RETURN_GENERATED_KEYS
                );
                PreparedStatement studentStmt = conn.prepareStatement(studentSql)
            ) {
                userStmt.setString(1, email);
                userStmt.setString(2, hashedPassword);
                userStmt.setString(3, "STUDENT");

                int userRowsInserted = userStmt.executeUpdate();
                if (userRowsInserted <= 0) {
                    conn.rollback();
                    return Optional.empty();
                }

                try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
                    if (!generatedKeys.next()) {
                        conn.rollback();
                        return Optional.empty();
                    }

                    long generatedUserId = generatedKeys.getLong(1);
                    student.setUserId(generatedUserId);
                }

                studentStmt.setString(1, student.getStudentId());
                studentStmt.setLong(2, student.getUserId());
                studentStmt.setString(3, student.getFirstName());
                studentStmt.setString(4, student.getLastName());
                studentStmt.setString(5, student.getMiddleName());
                studentStmt.setDate(6, new java.sql.Date(student.getBirthdate().getTime()));
                studentStmt.setString(
                    7,
                    student.getStudentStatus() == null
                        ? StudentStatus.REGULAR.name()
                        : student.getStudentStatus().name()
                );

                if (student.getCourseId() == null) {
                    studentStmt.setNull(8, java.sql.Types.BIGINT);
                } else {
                    studentStmt.setLong(8, student.getCourseId());
                }

                if (student.getYearLevel() == null) {
                    studentStmt.setInt(9, 1);
                } else {
                    studentStmt.setLong(9, student.getYearLevel());
                }

                int studentRowsInserted = studentStmt.executeUpdate();
                if (studentRowsInserted <= 0) {
                    conn.rollback();
                    return Optional.empty();
                }

                conn.commit();
                return Optional.of(student);
            } catch (SQLException e) {
                conn.rollback();
                logger.error(e.getMessage());
                return Optional.empty();
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<Student> update(Student student) {
        String sql =
            "UPDATE students SET first_name = ?, last_name = ?, middle_name = ?, birthdate = ?, student_status = ?, course_id = ?, year_level = ?, updated_at = CURRENT_TIMESTAMP WHERE student_id = ?";

        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, student.getFirstName());
            stmt.setString(2, student.getLastName());
            stmt.setString(3, student.getMiddleName());
            stmt.setDate(4, new java.sql.Date(student.getBirthdate().getTime()));
            stmt.setString(
                5,
                student.getStudentStatus() == null
                    ? StudentStatus.REGULAR.name()
                    : student.getStudentStatus().name()
            );

            if (student.getCourseId() == null) {
                stmt.setNull(6, java.sql.Types.BIGINT);
            } else {
                stmt.setLong(6, student.getCourseId());
            }

            if (student.getYearLevel() == null) {
                stmt.setNull(7, java.sql.Types.INTEGER);
            } else {
                stmt.setLong(7, student.getYearLevel());
            }

            stmt.setString(8, student.getStudentId());

            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                return Optional.of(student);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return Optional.empty();
    }

    public Optional<String> getUserEmailByUserId(Long userId) {
        String sql = "SELECT email FROM users WHERE id = ?";
        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.ofNullable(rs.getString("email"));
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return Optional.empty();
    }

    public String generateStudentId() {
        int currentYear = Year.now().getValue();

        for (int i = 0; i < 50; i++) {
            int randomNumber = ThreadLocalRandom.current().nextInt(10000, 100000);
            String candidate = currentYear + "-" + randomNumber;
            if (get(candidate).isEmpty()) {
                return candidate;
            }
        }

        String sql = "SELECT COUNT(*) AS total FROM students";
        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery()
        ) {
            if (rs.next()) {
                int nextCount = rs.getInt("total") + 1;
                return String.format("%d-%05d", currentYear, nextCount);
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return currentYear + "-99999";
    }

    public void delete(String studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";

        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setString(1, studentId);
            int rowsDeleted = stmt.executeUpdate();

            if (rowsDeleted > 0) {
                logger.info(
                    "Student with ID %s was successfully deleted!",
                    studentId
                );
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }

    public Optional<Student> getStudentByUserId(Long userId) {
        String sql = "SELECT * FROM students WHERE user_id = ?";
        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    if (rs.getRow() > 0) {
                        return Optional.of(StudentMapper.mapToStudent(rs));
                    } else {
                        return Optional.empty();
                    }
                }
            }
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }

        return Optional.empty();
    }

    public List<Student> list() {
        String sql = "SELECT * FROM students";
        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            List<Student> students = new ArrayList<>();
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    students.add(
                        new Student(
                            rs.getString("student_id"),
                            rs.getLong("user_id"),
                            rs.getString("first_name"),
                            rs.getString("last_name"),
                            rs.getString("middle_name"),
                            rs.getDate("birthdate"),
                            StudentStatus.valueOf(
                                rs.getString("student_status")
                            ),
                            rs.getLong("course_id"),
                            rs.getLong("year_level"),
                            rs.getTimestamp("created_at")
                        )
                    );
                }
            }

            return students;
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return List.of(); // Placeholder muna
    }

    public void deleteAll(List<Student> students) {
        String sql = "DELETE FROM students WHERE student_id = ?";

        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement stmt = conn.prepareStatement(sql)
        ) {
            for (Student student : students) {
                stmt.setString(1, student.getStudentId());
                stmt.addBatch();
            }

            int[] rowsDeleted = stmt.executeBatch();
            logger.info(
                "%d students were successfully deleted!",
                rowsDeleted.length
            );
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
    }
}
