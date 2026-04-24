package com.group5.paul_esys.modules.enrollments.utils;

import com.group5.paul_esys.modules.enums.SemesterProgressStatus;
import com.group5.paul_esys.modules.enrollments.model.StudentSemesterProgress;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class StudentSemesterProgressUtils {

  public static StudentSemesterProgress mapResultSetToStudentSemesterProgress(ResultSet rs) throws SQLException {
    return new StudentSemesterProgress(
        getNullableLong(rs, "id"),
        rs.getString("student_id"),
        getNullableLong(rs, "curriculum_id"),
        getNullableLong(rs, "semester_id"),
        parseStatus(rs.getString("status")),
        rs.getTimestamp("started_at"),
        rs.getTimestamp("completed_at"),
        rs.getTimestamp("created_at"),
        rs.getTimestamp("updated_at")
    );
  }

  private static Long getNullableLong(ResultSet rs, String column) throws SQLException {
    long value = rs.getLong(column);
    return rs.wasNull() ? null : value;
  }

  private static SemesterProgressStatus parseStatus(String rawStatus) {
    if (rawStatus == null || rawStatus.isBlank()) {
      return SemesterProgressStatus.NOT_STARTED;
    }

    String normalized = rawStatus
        .trim()
        .toUpperCase(Locale.ROOT)
        .replace('-', '_')
        .replace(' ', '_');

    try {
      return SemesterProgressStatus.valueOf(normalized);
    } catch (IllegalArgumentException ex) {
      return SemesterProgressStatus.NOT_STARTED;
    }
  }
}