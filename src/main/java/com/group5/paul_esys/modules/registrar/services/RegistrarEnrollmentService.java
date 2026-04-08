package com.group5.paul_esys.modules.registrar.services;

import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.registrar.model.EnrollmentApplication;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrarEnrollmentService {

  private static final RegistrarEnrollmentService INSTANCE = new RegistrarEnrollmentService();
  private static final Logger logger = LoggerFactory.getLogger(RegistrarEnrollmentService.class);

  private static final String APPLICATIONS_QUERY = """
      SELECT
        e.id AS enrollment_id,
        e.student_id,
        s.first_name,
        s.last_name,
        s.year_level,
        e.status
      FROM enrollments e
      INNER JOIN students s ON s.student_id = e.student_id
      ORDER BY e.created_at DESC
      """;

  private RegistrarEnrollmentService() {
  }

  public static RegistrarEnrollmentService getInstance() {
    return INSTANCE;
  }

  public List<EnrollmentApplication> getEnrollmentApplications() {
    List<EnrollmentApplication> applications = new ArrayList<>();

    try (Connection conn = ConnectionService.getConnection();
         PreparedStatement ps = conn.prepareStatement(APPLICATIONS_QUERY);
         ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        applications.add(mapRowToApplication(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
    }

    return applications;
  }

  public int countPendingActions() {
    return countByStatuses(List.of(EnrollmentStatus.SUBMITTED, EnrollmentStatus.DRAFT));
  }

  public int countApplicationsToReview() {
    return countByStatuses(List.of(EnrollmentStatus.SUBMITTED));
  }

  public int countTotalRegistered() {
    return countByStatuses(List.of(EnrollmentStatus.APPROVED, EnrollmentStatus.ENROLLED));
  }

  public int updateApplicationsStatus(List<Long> enrollmentIds, EnrollmentStatus targetStatus) {
    if (enrollmentIds == null || enrollmentIds.isEmpty()) {
      return 0;
    }

    int updatedCount = 0;

    try (Connection conn = ConnectionService.getConnection()) {
      conn.setAutoCommit(false);

      try (
          PreparedStatement updateEnrollmentStatusPs = conn.prepareStatement(
              "UPDATE enrollments SET status = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND status = ?"
          );
          PreparedStatement updateDetailsStatusPs = conn.prepareStatement(
              "UPDATE enrollments_details SET status = ? WHERE enrollment_id = ?"
          )
      ) {
        for (Long enrollmentId : enrollmentIds) {
          updateEnrollmentStatusPs.setString(1, targetStatus.name());
          updateEnrollmentStatusPs.setLong(2, enrollmentId);
          updateEnrollmentStatusPs.setString(3, EnrollmentStatus.SUBMITTED.name());

          int currentUpdateCount = updateEnrollmentStatusPs.executeUpdate();
          if (currentUpdateCount <= 0) {
            continue;
          }

          updateDetailsStatusPs.setString(1, resolveDetailStatusFor(targetStatus));
          updateDetailsStatusPs.setLong(2, enrollmentId);
          updateDetailsStatusPs.executeUpdate();

          updatedCount += currentUpdateCount;
        }

        conn.commit();
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      } finally {
        conn.setAutoCommit(true);
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return 0;
    }

    return updatedCount;
  }

  private int countByStatuses(List<EnrollmentStatus> statuses) {
    if (statuses == null || statuses.isEmpty()) {
      return 0;
    }

    StringBuilder placeholders = new StringBuilder();
    for (int i = 0; i < statuses.size(); i++) {
      placeholders.append('?');
      if (i < statuses.size() - 1) {
        placeholders.append(',');
      }
    }

    String sql = "SELECT COUNT(*) AS total FROM enrollments WHERE status IN (" + placeholders + ')';

    try (Connection conn = ConnectionService.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {

      for (int i = 0; i < statuses.size(); i++) {
        ps.setString(i + 1, statuses.get(i).name());
      }

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return rs.getInt("total");
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
    }

    return 0;
  }

  private EnrollmentApplication mapRowToApplication(ResultSet rs) throws SQLException {
    return new EnrollmentApplication()
        .setEnrollmentId(rs.getLong("enrollment_id"))
        .setStudentId(rs.getString("student_id"))
        .setFirstName(rs.getString("first_name"))
        .setLastName(rs.getString("last_name"))
        .setYearLevel(rs.getObject("year_level", Integer.class))
        .setStatus(EnrollmentStatus.valueOf(rs.getString("status")));
  }

  private String resolveDetailStatusFor(EnrollmentStatus targetStatus) {
    if (targetStatus == EnrollmentStatus.CANCELLED) {
      return EnrollmentDetailStatus.DROPPED.name();
    }
    return EnrollmentDetailStatus.SELECTED.name();
  }
}
