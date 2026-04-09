package com.group5.paul_esys.modules.semester_subjects.utils;

import com.group5.paul_esys.modules.semester_subjects.model.SemesterSubject;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SemesterSubjectUtils {

  public static SemesterSubject mapResultSetToSemesterSubject(ResultSet rs) throws SQLException {
    return new SemesterSubject(
      rs.getLong("id"),
      rs.getLong("semester_id"),
      rs.getLong("subject_id"),
      rs.getTimestamp("created_at"),
      rs.getTimestamp("updated_at")
    );
  }
}