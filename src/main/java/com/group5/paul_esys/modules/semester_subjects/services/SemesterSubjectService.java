package com.group5.paul_esys.modules.semester_subjects.services;

import com.group5.paul_esys.modules.semester_subjects.model.SemesterSubject;
import com.group5.paul_esys.modules.semester_subjects.utils.SemesterSubjectUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SemesterSubjectService {

  private static final SemesterSubjectService INSTANCE = new SemesterSubjectService();
  private static final Logger logger = LoggerFactory.getLogger(SemesterSubjectService.class);

  private SemesterSubjectService() {
  }

  public static SemesterSubjectService getInstance() {
    return INSTANCE;
  }

  public List<SemesterSubject> getAllSemesterSubjects() {
    List<SemesterSubject> semesterSubjects = new ArrayList<>();
    String sql = "SELECT * FROM semester_subjects ORDER BY semester_id, subject_id";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql);
      ResultSet rs = ps.executeQuery()
    ) {
      while (rs.next()) {
        semesterSubjects.add(SemesterSubjectUtils.mapResultSetToSemesterSubject(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return semesterSubjects;
  }

  public Optional<SemesterSubject> getSemesterSubjectById(Long id) {
    String sql = "SELECT * FROM semester_subjects WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, id);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(SemesterSubjectUtils.mapResultSetToSemesterSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public List<SemesterSubject> getSemesterSubjectsBySemester(Long semesterId) {
    List<SemesterSubject> semesterSubjects = new ArrayList<>();
    String sql = "SELECT * FROM semester_subjects WHERE semester_id = ? ORDER BY subject_id";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, semesterId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          semesterSubjects.add(SemesterSubjectUtils.mapResultSetToSemesterSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return semesterSubjects;
  }

  public List<SemesterSubject> getSemesterSubjectsBySubject(Long subjectId) {
    List<SemesterSubject> semesterSubjects = new ArrayList<>();
    String sql = "SELECT * FROM semester_subjects WHERE subject_id = ? ORDER BY semester_id";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, subjectId);
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          semesterSubjects.add(SemesterSubjectUtils.mapResultSetToSemesterSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return semesterSubjects;
  }

  public Optional<SemesterSubject> getBySemesterAndSubject(Long semesterId, Long subjectId) {
    String sql = "SELECT * FROM semester_subjects WHERE semester_id = ? AND subject_id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, semesterId);
      ps.setLong(2, subjectId);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(SemesterSubjectUtils.mapResultSetToSemesterSubject(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public boolean createSemesterSubject(SemesterSubject semesterSubject) {
    String sql = "INSERT INTO semester_subjects (semester_id, subject_id) VALUES (?, ?)";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, semesterSubject.getSemesterId());
      ps.setLong(2, semesterSubject.getSubjectId());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateSemesterSubject(SemesterSubject semesterSubject) {
    String sql = "UPDATE semester_subjects SET semester_id = ?, subject_id = ? WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, semesterSubject.getSemesterId());
      ps.setLong(2, semesterSubject.getSubjectId());
      ps.setLong(3, semesterSubject.getId());

      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteSemesterSubject(Long id) {
    String sql = "DELETE FROM semester_subjects WHERE id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, id);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteBySemesterAndSubject(Long semesterId, Long subjectId) {
    String sql = "DELETE FROM semester_subjects WHERE semester_id = ? AND subject_id = ?";

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, semesterId);
      ps.setLong(2, subjectId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}