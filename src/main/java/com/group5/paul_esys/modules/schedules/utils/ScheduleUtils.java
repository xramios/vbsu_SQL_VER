package com.group5.paul_esys.modules.schedules.utils;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.schedules.model.Schedule;

import java.sql.ResultSet;
import java.sql.SQLException;

public class ScheduleUtils {

  public static Schedule mapResultSetToSchedule(ResultSet rs) throws SQLException {
    return new Schedule(
        rs.getLong("id"),
        rs.getLong("section_id"),
        rs.getLong("room_id"),
        rs.getLong("faculty_id"),
        DayOfWeek.valueOf(rs.getString("day")),
        rs.getTime("start_time"),
        rs.getTime("end_time"),
        rs.getString("school_year"),
        rs.getInt("semester"),
        rs.getTimestamp("updated_at"),
        rs.getTimestamp("created_at")
    );
  }
}
