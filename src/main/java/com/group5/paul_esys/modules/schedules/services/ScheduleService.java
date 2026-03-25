package com.group5.paul_esys.modules.schedules.services;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.schedules.model.Schedule;
import com.group5.paul_esys.modules.schedules.utils.ScheduleUtils;
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

public class ScheduleService {

  private final Connection conn = ConnectionService.getConnection();
  private final Logger logger = LoggerFactory.getLogger(ScheduleService.class);

  public List<Schedule> getAllSchedules() {
    List<Schedule> schedules = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules ORDER BY day, start_time");
      ResultSet rs = ps.executeQuery();
      
      while (rs.next()) {
        schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public Optional<Schedule> getScheduleById(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE id = ?");
      ps.setLong(1, id);
      
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return Optional.of(ScheduleUtils.mapResultSetToSchedule(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Schedule> getSchedulesBySection(Long sectionId) {
    List<Schedule> schedules = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE section_id = ? ORDER BY day, start_time");
      ps.setLong(1, sectionId);
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public List<Schedule> getSchedulesByFaculty(Long facultyId) {
    List<Schedule> schedules = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE faculty_id = ? ORDER BY day, start_time");
      ps.setLong(1, facultyId);
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public List<Schedule> getSchedulesByRoom(Long roomId) {
    List<Schedule> schedules = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE room_id = ? ORDER BY day, start_time");
      ps.setLong(1, roomId);
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public List<Schedule> getSchedulesByDay(DayOfWeek day) {
    List<Schedule> schedules = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM schedules WHERE day = ? ORDER BY start_time");
      ps.setString(1, day.name());
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        schedules.add(ScheduleUtils.mapResultSetToSchedule(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return schedules;
  }

  public boolean createSchedule(Schedule schedule) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO schedules (section_id, room_id, faculty_id, day, start_time, end_time, school_year, semester) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
      );
      ps.setLong(1, schedule.getSectionId());
      ps.setLong(2, schedule.getRoomId());
      ps.setLong(3, schedule.getFacultyId());
      ps.setString(4, schedule.getDay().name());
      ps.setTime(5, schedule.getStartTime());
      ps.setTime(6, schedule.getEndTime());
      ps.setString(7, schedule.getSchoolYear());
      ps.setInt(8, schedule.getSemester());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateSchedule(Schedule schedule) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "UPDATE schedules SET section_id = ?, room_id = ?, faculty_id = ?, day = ?, start_time = ?, end_time = ?, school_year = ?, semester = ? WHERE id = ?"
      );
      ps.setLong(1, schedule.getSectionId());
      ps.setLong(2, schedule.getRoomId());
      ps.setLong(3, schedule.getFacultyId());
      ps.setString(4, schedule.getDay().name());
      ps.setTime(5, schedule.getStartTime());
      ps.setTime(6, schedule.getEndTime());
      ps.setString(7, schedule.getSchoolYear());
      ps.setInt(8, schedule.getSemester());
      ps.setLong(9, schedule.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteSchedule(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("DELETE FROM schedules WHERE id = ?");
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
