package com.group5.paul_esys.modules.prerequisites.utils;

import com.group5.paul_esys.modules.prerequisites.model.Prerequisite;

import java.sql.ResultSet;
import java.sql.SQLException;

public class PrerequisiteUtils {

  public static Prerequisite mapResultSetToPrerequisite(ResultSet rs) throws SQLException {
    return new Prerequisite(
        rs.getLong("id"),
        rs.getLong("pre_subject_id"),
        rs.getLong("subject_id"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
