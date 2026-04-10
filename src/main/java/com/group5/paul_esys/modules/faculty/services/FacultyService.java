package com.group5.paul_esys.modules.faculty.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.utils.FacultyUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacultyService {

  private static final FacultyService INSTANCE = new FacultyService();
  private static final Logger logger = LoggerFactory.getLogger(FacultyService.class);

  private FacultyService() {
  }

  public static FacultyService getInstance() {
    return INSTANCE;
  }

  public List<Faculty> getAllFaculty() {
    List<Faculty> faculty = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculty ORDER BY last_name, first_name");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        faculty.add(FacultyUtils.mapResultSetToFaculty(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return faculty;
  }

  public Optional<Faculty> getFacultyById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculty WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(FacultyUtils.mapResultSetToFaculty(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<Faculty> getFacultyByUserId(Long userId) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculty WHERE user_id = ?")) {
      ps.setLong(1, userId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(FacultyUtils.mapResultSetToFaculty(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Faculty> getFacultyByDepartment(Long departmentId) {
    List<Faculty> faculty = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM faculty WHERE department_id = ? ORDER BY last_name, first_name")) {
      ps.setLong(1, departmentId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          faculty.add(FacultyUtils.mapResultSetToFaculty(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return faculty;
  }

  public boolean createFaculty(Faculty faculty) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "INSERT INTO faculty (user_id, first_name, last_name, middle_name, contact_number, birthdate, department_id) VALUES (?, ?, ?, ?, ?, ?, ?)"
        )) {
      if (faculty.getUserId() == null) {
        ps.setNull(1, java.sql.Types.BIGINT);
      } else {
        ps.setLong(1, faculty.getUserId());
      }
      ps.setString(2, faculty.getFirstName());
      ps.setString(3, faculty.getLastName());
      ps.setString(4, faculty.getMiddleName());
      ps.setString(5, faculty.getContactNumber());
      if (faculty.getBirthdate() == null) {
        ps.setNull(6, java.sql.Types.DATE);
      } else {
        ps.setDate(6, new java.sql.Date(faculty.getBirthdate().getTime()));
      }
      if (faculty.getDepartmentId() == null) {
        ps.setNull(7, java.sql.Types.BIGINT);
      } else {
        ps.setLong(7, faculty.getDepartmentId());
      }
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public Optional<Faculty> registerFaculty(String email, String plainPassword, Faculty faculty) {
    String userSql = "INSERT INTO users (email, password, role) VALUES (?, ?, ?)";
    String facultySql = "INSERT INTO faculty (user_id, first_name, last_name, middle_name, contact_number, birthdate, department_id) VALUES (?, ?, ?, ?, ?, ?, ?)";

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
        PreparedStatement facultyStmt = conn.prepareStatement(facultySql)
      ) {
        userStmt.setString(1, email);
        userStmt.setString(2, hashedPassword);
        userStmt.setString(3, "FACULTY");

        if (userStmt.executeUpdate() <= 0) {
          conn.rollback();
          return Optional.empty();
        }

        try (ResultSet generatedKeys = userStmt.getGeneratedKeys()) {
          if (!generatedKeys.next()) {
            conn.rollback();
            return Optional.empty();
          }

          faculty.setUserId(generatedKeys.getLong(1));
        }

        facultyStmt.setLong(1, faculty.getUserId());
        facultyStmt.setString(2, faculty.getFirstName());
        facultyStmt.setString(3, faculty.getLastName());
        facultyStmt.setString(4, faculty.getMiddleName());
        facultyStmt.setString(5, faculty.getContactNumber());
        if (faculty.getBirthdate() == null) {
          facultyStmt.setNull(6, java.sql.Types.DATE);
        } else {
          facultyStmt.setDate(6, new java.sql.Date(faculty.getBirthdate().getTime()));
        }
        if (faculty.getDepartmentId() == null) {
          facultyStmt.setNull(7, java.sql.Types.BIGINT);
        } else {
          facultyStmt.setLong(7, faculty.getDepartmentId());
        }

        if (facultyStmt.executeUpdate() <= 0) {
          conn.rollback();
          return Optional.empty();
        }

        conn.commit();
        return Optional.of(faculty);
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR: " + e.getMessage(), e);
        return Optional.empty();
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return Optional.empty();
    }
  }

  public Optional<String> getUserEmailByUserId(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement("SELECT email FROM users WHERE id = ?")
    ) {
      ps.setLong(1, userId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("email"));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public boolean updateFacultyWithEmail(Faculty faculty, String email) {
    if (faculty == null || faculty.getId() == null) {
      return false;
    }

    String facultySql =
      "UPDATE faculty SET user_id = ?, first_name = ?, last_name = ?, middle_name = ?, contact_number = ?, birthdate = ?, department_id = ? WHERE id = ?";
    String userSql = "UPDATE users SET email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);

      try (
        PreparedStatement facultyStmt = conn.prepareStatement(facultySql);
        PreparedStatement userStmt = faculty.getUserId() == null
          ? null
          : conn.prepareStatement(userSql)
      ) {
        if (faculty.getUserId() == null) {
          facultyStmt.setNull(1, java.sql.Types.BIGINT);
        } else {
          facultyStmt.setLong(1, faculty.getUserId());
        }
        facultyStmt.setString(2, faculty.getFirstName());
        facultyStmt.setString(3, faculty.getLastName());
        facultyStmt.setString(4, faculty.getMiddleName());
        facultyStmt.setString(5, faculty.getContactNumber());
        if (faculty.getBirthdate() == null) {
          facultyStmt.setNull(6, java.sql.Types.DATE);
        } else {
          facultyStmt.setDate(6, new java.sql.Date(faculty.getBirthdate().getTime()));
        }
        if (faculty.getDepartmentId() == null) {
          facultyStmt.setNull(7, java.sql.Types.BIGINT);
        } else {
          facultyStmt.setLong(7, faculty.getDepartmentId());
        }
        facultyStmt.setLong(8, faculty.getId());

        if (facultyStmt.executeUpdate() <= 0) {
          conn.rollback();
          return false;
        }

        if (userStmt != null) {
          userStmt.setString(1, email);
          userStmt.setLong(2, faculty.getUserId());

          if (userStmt.executeUpdate() <= 0) {
            conn.rollback();
            return false;
          }
        }

        conn.commit();
        return true;
      } catch (SQLException e) {
        conn.rollback();
        logger.error("ERROR: " + e.getMessage(), e);
        return false;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateFaculty(Faculty faculty) {
    try (Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(
        "UPDATE faculty SET user_id = ?, first_name = ?, last_name = ?, middle_name = ?, contact_number = ?, birthdate = ?, department_id = ? WHERE id = ?"
      )) {
      if (faculty.getUserId() == null) {
        ps.setNull(1, java.sql.Types.BIGINT);
      } else {
        ps.setLong(1, faculty.getUserId());
      }
      ps.setString(2, faculty.getFirstName());
      ps.setString(3, faculty.getLastName());
      ps.setString(4, faculty.getMiddleName());
      ps.setString(5, faculty.getContactNumber());
      if (faculty.getBirthdate() == null) {
        ps.setNull(6, java.sql.Types.DATE);
      } else {
        ps.setDate(6, new java.sql.Date(faculty.getBirthdate().getTime()));
      }
      if (faculty.getDepartmentId() == null) {
        ps.setNull(7, java.sql.Types.BIGINT);
      } else {
        ps.setLong(7, faculty.getDepartmentId());
      }
      ps.setLong(8, faculty.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteFaculty(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("DELETE FROM faculty WHERE id = ?")) {
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
