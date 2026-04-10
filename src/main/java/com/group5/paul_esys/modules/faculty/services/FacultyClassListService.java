package com.group5.paul_esys.modules.faculty.services;

import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.faculty.model.FacultyClassListAggregate;
import com.group5.paul_esys.modules.faculty.model.FacultyClassListRow;
import com.group5.paul_esys.modules.faculty.model.FacultyClassStudentRow;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacultyClassListService {

  private static final FacultyClassListService INSTANCE = new FacultyClassListService();
  private static final Logger logger = LoggerFactory.getLogger(FacultyClassListService.class);
  private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

  private FacultyClassListService() {
  }

  public static FacultyClassListService getInstance() {
    return INSTANCE;
  }

  public List<FacultyClassListRow> getClassListRowsByFaculty(Long facultyId) {
    if (facultyId == null) {
      return List.of();
    }

    String sql = """
      SELECT
        o.id AS offering_id,
        sub.subject_code,
        sub.subject_name,
        sec.section_code,
        sch.day,
        sch.start_time,
        sch.end_time,
        rm.room AS room_name,
        COALESCE(student_count.total_students, 0) AS total_students
      FROM offerings o
      INNER JOIN subjects sub ON sub.id = o.subject_id
      INNER JOIN sections sec ON sec.id = o.section_id
      INNER JOIN schedules sch ON sch.offering_id = o.id
      LEFT JOIN rooms rm ON rm.id = sch.room_id
      LEFT JOIN (
        SELECT
          ed.offering_id,
          COUNT(*) AS total_students
        FROM enrollments_details ed
        INNER JOIN enrollments e ON e.id = ed.enrollment_id
        WHERE ed.status = ?
          AND e.status IN (?, ?)
        GROUP BY ed.offering_id
      ) student_count ON student_count.offering_id = o.id
      WHERE sch.faculty_id = ?
      ORDER BY
        sub.subject_code,
        sec.section_code,
        CASE sch.day
          WHEN 'MON' THEN 1
          WHEN 'TUE' THEN 2
          WHEN 'WED' THEN 3
          WHEN 'THU' THEN 4
          WHEN 'FRI' THEN 5
          WHEN 'SAT' THEN 6
          WHEN 'SUN' THEN 7
          ELSE 8
        END,
        sch.start_time
      """;

    Map<Long, FacultyClassListAggregate> rowsByOfferingId = new LinkedHashMap<>();

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setString(1, EnrollmentDetailStatus.SELECTED.name());
      ps.setString(2, EnrollmentStatus.APPROVED.name());
      ps.setString(3, EnrollmentStatus.ENROLLED.name());
      ps.setLong(4, facultyId);

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Long offeringId = rsGetLong(rs, "offering_id");
          if (offeringId == null) {
            continue;
          }

          FacultyClassListAggregate aggregate = rowsByOfferingId.get(offeringId);
          if (aggregate == null) {
            aggregate = new FacultyClassListAggregate(
                offeringId,
                safeText(rs.getString("subject_code"), "N/A"),
                safeText(rs.getString("subject_name"), "N/A"),
                safeText(rs.getString("section_code"), "N/A"),
                safeInt(rs.getObject("total_students", Integer.class), 0)
            );
            rowsByOfferingId.put(offeringId, aggregate);
          }

          appendScheduleMetadata(rs, aggregate);
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    List<FacultyClassListRow> rows = new ArrayList<>();
    for (FacultyClassListAggregate aggregate : rowsByOfferingId.values()) {
      rows.add(aggregate.toRow());
    }
    return rows;
  }

  public List<FacultyClassStudentRow> getClassStudentsByOffering(Long offeringId) {
    if (offeringId == null) {
      return List.of();
    }

    String sql = """
      SELECT
        st.last_name,
        st.first_name,
        st.middle_name,
        st.student_status,
        c.course_name,
        cur.name AS curriculum_name,
        st.year_level
      FROM enrollments_details ed
      INNER JOIN enrollments e ON e.id = ed.enrollment_id
      INNER JOIN students st ON st.student_id = e.student_id
      LEFT JOIN courses c ON c.id = st.course_id
      LEFT JOIN curriculum cur ON cur.id = st.curriculum_id
      WHERE ed.offering_id = ?
        AND ed.status = ?
        AND e.status IN (?, ?)
      ORDER BY st.last_name, st.first_name, st.middle_name
      """;

    List<FacultyClassStudentRow> students = new ArrayList<>();

    try (
      Connection conn = ConnectionService.getConnection();
      PreparedStatement ps = conn.prepareStatement(sql)
    ) {
      ps.setLong(1, offeringId);
      ps.setString(2, EnrollmentDetailStatus.SELECTED.name());
      ps.setString(3, EnrollmentStatus.APPROVED.name());
      ps.setString(4, EnrollmentStatus.ENROLLED.name());

      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          Integer yearLevel = rs.getObject("year_level", Integer.class);
          String yearLevelValue = yearLevel == null ? null : String.valueOf(yearLevel);

          students.add(new FacultyClassStudentRow(
              buildStudentFullName(
                  rs.getString("last_name"),
                  rs.getString("first_name"),
                  rs.getString("middle_name")
              ),
              safeText(rs.getString("student_status"), "N/A"),
              safeText(rs.getString("course_name"), "N/A"),
              safeText(rs.getString("curriculum_name"), "N/A"),
              safeText(yearLevelValue, "N/A")
          ));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: {}", e.getMessage(), e);
      return List.of();
    }

    return students;
  }

  private void appendScheduleMetadata(ResultSet rs, FacultyClassListAggregate aggregate) throws SQLException {
    String day = rs.getString("day");
    Time startTime = rs.getTime("start_time");
    Time endTime = rs.getTime("end_time");

    if (day != null && startTime != null && endTime != null) {
      aggregate.scheduleParts().add(day + " " + formatTime(startTime) + "-" + formatTime(endTime));
    }

    String roomName = safeText(rs.getString("room_name"), "");
    if (!roomName.isBlank()) {
      aggregate.roomParts().add(roomName);
    }
  }

  private String buildStudentFullName(String lastName, String firstName, String middleName) {
    String safeLastName = safeText(lastName, "");
    String safeFirstName = safeText(firstName, "");
    String safeMiddleName = safeText(middleName, "");

    StringBuilder fullNameBuilder = new StringBuilder();

    if (!safeLastName.isBlank()) {
      fullNameBuilder.append(safeLastName);
    }

    if (!safeFirstName.isBlank()) {
      if (fullNameBuilder.length() > 0) {
        fullNameBuilder.append(", ");
      }
      fullNameBuilder.append(safeFirstName);
    }

    if (!safeMiddleName.isBlank()) {
      if (fullNameBuilder.length() > 0) {
        fullNameBuilder.append(' ');
      }
      fullNameBuilder.append(safeMiddleName.charAt(0)).append('.');
    }

    if (fullNameBuilder.length() == 0) {
      return "N/A";
    }

    return fullNameBuilder.toString();
  }

  private String formatTime(Time time) {
    return time.toLocalTime().format(TIME_FORMATTER);
  }

  private int safeInt(Integer value, int fallback) {
    return value == null ? fallback : value;
  }

  private String safeText(String text, String fallback) {
    if (text == null || text.isBlank()) {
      return fallback;
    }

    return text.trim();
  }

  private static Long rsGetLong(ResultSet rs, String column) {
    try {
      long value = rs.getLong(column);
      return rs.wasNull() ? null : value;
    } catch (SQLException e) {
      return null;
    }
  }
}