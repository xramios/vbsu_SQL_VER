package com.group5.paul_esys.modules.enrollment_period.services;

import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.enrollment_period.utils.EnrollmentPeriodUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EnrollmentPeriodService {

  private static final EnrollmentPeriodService INSTANCE = new EnrollmentPeriodService();
  private static final Logger logger = LoggerFactory.getLogger(EnrollmentPeriodService.class);

  private EnrollmentPeriodService() {
  }

  public static EnrollmentPeriodService getInstance() {
    return INSTANCE;
  }

  private boolean hasDescriptionColumn(Connection conn) {
    try {
      DatabaseMetaData metadata = conn.getMetaData();
      try (ResultSet rs = metadata.getColumns(null, null, "ENROLLMENT_PERIOD", "DESCRIPTION")) {
        if (rs.next()) {
          return true;
        }
      }

      try (ResultSet rs = metadata.getColumns(null, null, "enrollment_period", "description")) {
        return rs.next();
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public List<EnrollmentPeriod> getAllEnrollmentPeriods() {
    List<EnrollmentPeriod> periods = new ArrayList<>();
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollment_period ORDER BY created_at DESC");
        ResultSet rs = ps.executeQuery()) {
      while (rs.next()) {
        periods.add(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return periods;
  }

  public Optional<EnrollmentPeriod> getEnrollmentPeriodById(Long id) {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT * FROM enrollment_period WHERE id = ?")) {
      ps.setLong(1, id);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<EnrollmentPeriod> getCurrentEnrollmentPeriod() {
    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(
            "SELECT * FROM enrollment_period WHERE start_date <= CURRENT_TIMESTAMP AND end_date >= CURRENT_TIMESTAMP"
        );
        ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        return Optional.of(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<EnrollmentPeriod> getLatestEnrollmentPeriod() {
    String sql = "SELECT * FROM enrollment_period ORDER BY created_at DESC";

    try (Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {
      if (rs.next()) {
        return Optional.of(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }

  public Optional<EnrollmentPeriod> getCurrentOrLatestEnrollmentPeriod() {
    Optional<EnrollmentPeriod> current = getCurrentEnrollmentPeriod();
    if (current.isPresent()) {
      return current;
    }

    return getLatestEnrollmentPeriod();
  }

  public Optional<EnrollmentPeriod> findConflictingEnrollmentPeriod(EnrollmentPeriod period) {
    if (period == null || period.getStartDate() == null || period.getEndDate() == null) {
      return Optional.empty();
    }

    try (Connection conn = ConnectionService.getConnection()) {
      return findConflictingEnrollmentPeriod(conn, period);
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return Optional.empty();
    }
  }

  public Optional<EnrollmentPeriod> getOpenEnrollmentPeriodExcluding(Long excludeId) {
    try (Connection conn = ConnectionService.getConnection()) {
      return findOpenEnrollmentPeriod(conn, excludeId);
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  private Optional<EnrollmentPeriod> findOpenEnrollmentPeriod(Connection conn, Long excludeId) throws SQLException {
    String sql = excludeId == null
        ? "SELECT * FROM enrollment_period WHERE start_date <= CURRENT_TIMESTAMP AND end_date >= CURRENT_TIMESTAMP"
        : "SELECT * FROM enrollment_period WHERE id <> ? AND start_date <= CURRENT_TIMESTAMP AND end_date >= CURRENT_TIMESTAMP";

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      if (excludeId != null) {
        ps.setLong(1, excludeId);
      }

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
        }
      }
    }

    return Optional.empty();
  }

  private Optional<EnrollmentPeriod> findConflictingEnrollmentPeriod(Connection conn, EnrollmentPeriod period)
      throws SQLException {
    String sql = period.getId() == null
        ? """
          SELECT *
          FROM enrollment_period
          WHERE start_date <= ?
            AND end_date >= ?
          ORDER BY start_date ASC
          FETCH FIRST 1 ROWS ONLY
          """
        : """
          SELECT *
          FROM enrollment_period
          WHERE id <> ?
            AND start_date <= ?
            AND end_date >= ?
          ORDER BY start_date ASC
          FETCH FIRST 1 ROWS ONLY
          """;

    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      int parameterIndex = 1;
      if (period.getId() != null) {
        ps.setLong(parameterIndex++, period.getId());
      }

      ps.setTimestamp(parameterIndex++, EnrollmentPeriodUtils.toSqlTimestamp(period.getEndDate()));
      ps.setTimestamp(parameterIndex, EnrollmentPeriodUtils.toSqlTimestamp(period.getStartDate()));

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(EnrollmentPeriodUtils.mapResultSetToEnrollmentPeriod(rs));
        }
      }
    }

    return Optional.empty();
  }

  public boolean createEnrollmentPeriod(EnrollmentPeriod period) {
    if (period == null
        || period.getStartDate() == null
        || period.getEndDate() == null
        || EnrollmentPeriodUtils.safeText(period.getSchoolYear(), null) == null
        || EnrollmentPeriodUtils.safeText(period.getSemester(), null) == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      if (findOpenEnrollmentPeriod(conn, null).isPresent()) {
        logger.warn("Enrollment period creation blocked because another period is already open.");
        return false;
      }

      if (findConflictingEnrollmentPeriod(conn, period).isPresent()) {
        logger.warn("Enrollment period conflict detected. schoolYear={}, semester={}, startDate={}, endDate={}",
            period.getSchoolYear(), period.getSemester(), period.getStartDate(), period.getEndDate());
        return false;
      }

      boolean hasDescription = hasDescriptionColumn(conn);
      String sql = hasDescription
          ? "INSERT INTO enrollment_period (school_year, semester, start_date, end_date, description) VALUES (?, ?, ?, ?, ?)"
          : "INSERT INTO enrollment_period (school_year, semester, start_date, end_date) VALUES (?, ?, ?, ?)";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, period.getSchoolYear());
        ps.setString(2, period.getSemester());
        ps.setTimestamp(3, EnrollmentPeriodUtils.toSqlTimestamp(period.getStartDate()));
        ps.setTimestamp(4, EnrollmentPeriodUtils.toSqlTimestamp(period.getEndDate()));

        if (hasDescription) {
          ps.setString(5, EnrollmentPeriodUtils.normalizeDescription(period.getDescription()));
        }

        return ps.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateEnrollmentPeriod(EnrollmentPeriod period) {
    if (period == null
        || period.getId() == null
        || period.getStartDate() == null
        || period.getEndDate() == null
        || EnrollmentPeriodUtils.safeText(period.getSchoolYear(), null) == null
        || EnrollmentPeriodUtils.safeText(period.getSemester(), null) == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      Optional<EnrollmentPeriod> openPeriod = findOpenEnrollmentPeriod(conn, period.getId());
      if (openPeriod.isPresent() && !Objects.equals(openPeriod.get().getId(), period.getId())) {
        logger.warn("Enrollment period update blocked because another period is currently open. updateId={}, openId={}",
            period.getId(), openPeriod.get().getId());
        return false;
      }

      if (findConflictingEnrollmentPeriod(conn, period).isPresent()) {
        logger.warn("Enrollment period conflict detected during update. id={}, schoolYear={}, semester={}, startDate={}, endDate={}",
            period.getId(), period.getSchoolYear(), period.getSemester(), period.getStartDate(), period.getEndDate());
        return false;
      }

      boolean hasDescription = hasDescriptionColumn(conn);
      String sql = hasDescription
          ? "UPDATE enrollment_period SET school_year = ?, semester = ?, start_date = ?, end_date = ?, description = ? WHERE id = ?"
          : "UPDATE enrollment_period SET school_year = ?, semester = ?, start_date = ?, end_date = ? WHERE id = ?";

      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, period.getSchoolYear());
        ps.setString(2, period.getSemester());
        ps.setTimestamp(3, EnrollmentPeriodUtils.toSqlTimestamp(period.getStartDate()));
        ps.setTimestamp(4, EnrollmentPeriodUtils.toSqlTimestamp(period.getEndDate()));

        if (hasDescription) {
          ps.setString(5, EnrollmentPeriodUtils.normalizeDescription(period.getDescription()));
          ps.setLong(6, period.getId());
        } else {
          ps.setLong(5, period.getId());
        }

        return ps.executeUpdate() > 0;
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteEnrollmentPeriod(Long id) {
    if (id == null) {
      return false;
    }

    try (Connection conn = ConnectionService.getConnection()) {
      Optional<EnrollmentPeriod> openPeriod = findOpenEnrollmentPeriod(conn, null);
      if (openPeriod.isPresent() && Objects.equals(openPeriod.get().getId(), id)) {
        logger.warn("Enrollment period delete blocked because the requested period is currently open. id={}", id);
        return false;
      }

      conn.setAutoCommit(false);
      try {
        // Step 1: Delete from student_enrolled_subjects through offerings
        try (PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM student_enrolled_subjects WHERE offering_id IN (SELECT id FROM offerings WHERE enrollment_period_id = ?)")) {
          ps.setLong(1, id);
          ps.executeUpdate();
        }

        // Step 2: Delete from offerings
        try (PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM offerings WHERE enrollment_period_id = ?")) {
          ps.setLong(1, id);
          ps.executeUpdate();
        }

        // Step 3: Delete the enrollment period itself
        try (PreparedStatement ps = conn.prepareStatement(
            "DELETE FROM enrollment_period WHERE id = ?")) {
          ps.setLong(1, id);
          
          boolean deleted = ps.executeUpdate() > 0;
          conn.commit();
          return deleted;
        }
      } catch (SQLException e) {
        conn.rollback();
        throw e;
      }
    } catch (SQLException e) {
      logger.error("ERROR deleting enrollment period " + id + ": " + e.getMessage(), e);
      return false;
    }
  }
}
