package com.group5.paul_esys.modules.faculty.utils;

import com.group5.paul_esys.modules.faculty.model.Faculty;

import java.sql.ResultSet;
import java.sql.SQLException;

public class FacultyUtils {

  public static Faculty mapResultSetToFaculty(ResultSet rs) throws SQLException {
    return new Faculty(
        rs.getLong("id"),
        rs.getLong("user_id"),
        rs.getString("first_name"),
        rs.getString("last_name"),
        rs.getLong("department_id"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
