package com.group5.paul_esys.modules.sections.utils;

import com.group5.paul_esys.modules.sections.model.Section;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SectionUtils {

  public static Section mapResultSetToSection(ResultSet rs) throws SQLException {
    return new Section(
        rs.getLong("id"),
        rs.getString("section_name"),
        rs.getString("section_code"),
        rs.getLong("subject_id"),
        rs.getInt("capacity"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
