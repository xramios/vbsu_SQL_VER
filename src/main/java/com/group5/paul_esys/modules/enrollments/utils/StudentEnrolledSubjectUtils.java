package com.group5.paul_esys.modules.enrollments.utils;

import com.group5.paul_esys.modules.enums.StudentEnrolledSubjectStatus;
import com.group5.paul_esys.modules.enrollments.model.StudentEnrolledSubject;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class StudentEnrolledSubjectUtils {

  public static StudentEnrolledSubject mapResultSetToStudentEnrolledSubject(ResultSet rs) throws SQLException {
    boolean selected = false;
    try {
      selected = rs.getBoolean("is_selected");
      if (rs.wasNull()) {
        selected = false;
      }
    } catch (SQLException ignored) {
      selected = false;
    }

    return new StudentEnrolledSubject(
        rs.getString("student_id"),
        getNullableLong(rs, "enrollment_id"),
        getNullableLong(rs, "offering_id"),
        getNullableLong(rs, "semester_subject_id"),
        parseStatus(rs.getString("status")),
        selected,
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at")
    );
  }

  private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private static StudentEnrolledSubjectStatus parseStatus(String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return StudentEnrolledSubjectStatus.ENROLLED;
    }

    String normalized = rawStatus
        .trim()
        .toUpperCase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_');

    try {
      return StudentEnrolledSubjectStatus.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return StudentEnrolledSubjectStatus.ENROLLED;
    }
  }
}