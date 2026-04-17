package com.group5.paul_esys.modules.registrar.services;

import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enrollments.services.StudentSemesterProgressService;
import com.group5.paul_esys.modules.registrar.model.*;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegistrarStudentScheduleService {

    private static final RegistrarStudentScheduleService INSTANCE = new RegistrarStudentScheduleService();
    private static final Logger logger = LoggerFactory.getLogger(RegistrarStudentScheduleService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private RegistrarStudentScheduleService() {
    }

    public static RegistrarStudentScheduleService getInstance() {
        return INSTANCE;
    }

    public List<StudentScheduleRow> getStudentSchedules(String studentId) {
        return getStudentSchedules(studentId, null);
    }

    public List<DuplicateSubjectDTO> getDuplicateSubjects(String studentId, Long enrollmentId) {
        List<StudentScheduleRow> schedules = getStudentSchedules(studentId, enrollmentId);
        Map<Long, List<StudentScheduleRow>> bySubject = new java.util.LinkedHashMap<>();
        for (StudentScheduleRow row : schedules) {
            bySubject.computeIfAbsent(row.subjectId(), k -> new ArrayList<>()).add(row);
        }
        
        List<DuplicateSubjectDTO> duplicates = new ArrayList<>();
        for (List<StudentScheduleRow> list : bySubject.values()) {
            if (list.size() > 1) {
                for (StudentScheduleRow row : list) {
                    duplicates.add(new DuplicateSubjectDTO(
                        row.enrollmentDetailId(),
                        row.subjectCode(),
                        row.sectionCode(),
                        row.instructor() != null && !row.instructor().isBlank() ? row.instructor() : "TBA",
                        "Enrollment Request"
                    ));
                }
            }
        }
        return duplicates;
    }

    public List<StudentScheduleRow> getStudentSchedules(String studentId, Long enrollmentId) {
        if (studentId == null || studentId.isBlank()) {
            return List.of();
        }

        Optional<EnrollmentSnapshot> enrollmentSnapshot = enrollmentId == null
            ? getLatestEnrollmentSnapshot(studentId)
            : getEnrollmentSnapshot(studentId, enrollmentId);

        if (enrollmentSnapshot.isEmpty()) {
            return List.of();
        }

        String sql = """
            SELECT
              ed.id AS enrollment_detail_id,
              ed.enrollment_id,
              ed.offering_id,
              ed.units,
              o.subject_id,
              o.section_id,
              o.enrollment_period_id,
              sub.subject_code,
              sub.subject_name,
              sec.section_code,
              sch.day,
              sch.start_time,
              sch.end_time,
              rm.room AS room_name,
              fac.first_name AS faculty_first_name,
              fac.last_name AS faculty_last_name
            FROM enrollments_details ed
            INNER JOIN offerings o ON o.id = ed.offering_id
            INNER JOIN subjects sub ON sub.id = o.subject_id
            INNER JOIN sections sec ON sec.id = o.section_id
            LEFT JOIN schedules sch ON sch.offering_id = o.id
            LEFT JOIN rooms rm ON rm.id = sch.room_id
            LEFT JOIN faculty fac ON fac.id = sch.faculty_id
            WHERE ed.enrollment_id = ?
              AND ed.status = ?
            ORDER BY ed.created_at, sch.day, sch.start_time
            """;

        Map<Long, StudentScheduleAggregate> rowsByDetailId = new LinkedHashMap<>();

        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setLong(1, enrollmentSnapshot.get().enrollmentId());
            ps.setString(2, EnrollmentDetailStatus.SELECTED.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long detailId = rs.getLong("enrollment_detail_id");
                    StudentScheduleAggregate aggregate = rowsByDetailId.get(detailId);
                    if (aggregate == null) {
                        aggregate = new StudentScheduleAggregate(
                            detailId,
                            rsGetLong(rs, "enrollment_id"),
                            rsGetLong(rs, "offering_id"),
                            rsGetLong(rs, "subject_id"),
                            rsGetLong(rs, "enrollment_period_id"),
                            safeText(rs.getString("subject_code"), "N/A"),
                            safeText(rs.getString("subject_name"), "N/A"),
                            safeText(rs.getString("section_code"), "N/A"),
                            rs.getObject("units", Float.class)
                        );
                        rowsByDetailId.put(detailId, aggregate);
                    }

                    appendScheduleMetadata(
                        rs,
                        aggregate.scheduleParts(),
                        aggregate.roomParts(),
                        aggregate.instructorParts()
                    );
                }
            }
        } catch (SQLException e) {
            logger.error("ERROR: {}", e.getMessage(), e);
            return List.of();
        }

        List<StudentScheduleRow> schedules = new ArrayList<>();
        for (StudentScheduleAggregate aggregate : rowsByDetailId.values()) {
            schedules.add(aggregate.toRow());
        }

        return schedules;
    }

    private Optional<EnrollmentSnapshot> getEnrollmentSnapshot(String studentId, Long enrollmentId) {
        String sql = """
            SELECT id, enrollment_period_id
            FROM enrollments
            WHERE student_id = ?
              AND id = ?
            FETCH FIRST 1 ROWS ONLY
            """;

        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, studentId);
            ps.setLong(2, enrollmentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new EnrollmentSnapshot(rsGetLong(rs, "id"), rsGetLong(rs, "enrollment_period_id")));
                }
            }
        } catch (SQLException e) {
            logger.error("ERROR: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    public List<SectionScheduleOption> getAvailableSectionSchedules(
        Long enrollmentPeriodId,
        Long subjectId,
        Long excludedOfferingId
    ) {
        if (enrollmentPeriodId == null || subjectId == null) {
            return List.of();
        }

        String sql = """
            SELECT
              o.id AS offering_id,
              o.capacity AS offering_capacity,
              sec.capacity AS section_capacity,
              sec.section_code,
              sch.day,
              sch.start_time,
              sch.end_time,
              rm.room AS room_name,
              fac.first_name AS faculty_first_name,
              fac.last_name AS faculty_last_name
            FROM offerings o
            INNER JOIN sections sec ON sec.id = o.section_id
            LEFT JOIN schedules sch ON sch.offering_id = o.id
            LEFT JOIN rooms rm ON rm.id = sch.room_id
            LEFT JOIN faculty fac ON fac.id = sch.faculty_id
            WHERE o.enrollment_period_id = ?
              AND o.subject_id = ?
            ORDER BY sec.section_code, sch.day, sch.start_time
            """;

        Map<Long, SectionScheduleAggregate> optionsByOffering = new LinkedHashMap<>();

        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setLong(1, enrollmentPeriodId);
            ps.setLong(2, subjectId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Long offeringId = rsGetLong(rs, "offering_id");
                    SectionScheduleAggregate aggregate = optionsByOffering.get(offeringId);
                    if (aggregate == null) {
                        aggregate = new SectionScheduleAggregate(
                            offeringId,
                            safeText(rs.getString("section_code"), "N/A"),
                            rs.getObject("offering_capacity", Integer.class),
                            rs.getObject("section_capacity", Integer.class)
                        );
                        optionsByOffering.put(offeringId, aggregate);
                    }

                    appendScheduleMetadata(
                        rs,
                        aggregate.scheduleParts(),
                        aggregate.roomParts(),
                        aggregate.instructorParts()
                    );
                }
            }

            Map<Long, Integer> selectedCountByOfferingId = getSelectedCountByOffering(conn, optionsByOffering.keySet());
            List<SectionScheduleOption> options = new ArrayList<>();

            for (SectionScheduleAggregate aggregate : optionsByOffering.values()) {
                if (excludedOfferingId != null && excludedOfferingId.equals(aggregate.offeringId())) {
                    continue;
                }

                Integer effectiveCapacity = aggregate.offeringCapacity() != null ? aggregate.offeringCapacity() : aggregate.sectionCapacity();
                Integer selectedCount = selectedCountByOfferingId.getOrDefault(aggregate.offeringId(), 0);
                Integer availableSlots = null;

                if (effectiveCapacity != null && effectiveCapacity > 0) {
                    availableSlots = Math.max(effectiveCapacity - selectedCount, 0);
                    if (availableSlots <= 0) {
                        continue;
                    }
                }

                options.add(new SectionScheduleOption(
                    aggregate.offeringId(),
                    aggregate.sectionCode(),
                    aggregate.instructorParts().isEmpty() ? "TBA" : String.join(", ", aggregate.instructorParts()),
                    aggregate.scheduleParts().isEmpty() ? "TBA" : String.join(" | ", aggregate.scheduleParts()),
                    aggregate.roomParts().isEmpty() ? "TBA" : String.join(", ", aggregate.roomParts()),
                    availableSlots
                ));
            }

            return options;
        } catch (SQLException e) {
            logger.error("ERROR: {}", e.getMessage(), e);
            return List.of();
        }
    }

    public SectionChangeResult changeStudentSection(String studentId, Long enrollmentDetailId, Long targetOfferingId) {
        if (studentId == null || studentId.isBlank() || enrollmentDetailId == null || targetOfferingId == null) {
            return new SectionChangeResult(false, "Invalid section change request.");
        }

        SourceSelection sourceSelection;

        try (Connection conn = ConnectionService.getConnection()) {
            conn.setAutoCommit(false);

            try {
                Optional<SourceSelection> sourceOptional = getSourceSelection(conn, studentId, enrollmentDetailId);
                if (sourceOptional.isEmpty()) {
                    conn.rollback();
                    return new SectionChangeResult(false, "The selected schedule could not be found.");
                }

                sourceSelection = sourceOptional.get();

                if (sourceSelection.offeringId().equals(targetOfferingId)) {
                    conn.rollback();
                    return new SectionChangeResult(false, "Please choose a different section.");
                }

                Optional<TargetSelection> targetOptional = getTargetSelection(conn, targetOfferingId);
                if (targetOptional.isEmpty()) {
                    conn.rollback();
                    return new SectionChangeResult(false, "The target section is no longer available.");
                }

                TargetSelection targetSelection = targetOptional.get();

                if (!sourceSelection.subjectId().equals(targetSelection.subjectId())) {
                    conn.rollback();
                    return new SectionChangeResult(false, "Section change is only allowed within the same subject.");
                }

                if (!sourceSelection.enrollmentPeriodId().equals(targetSelection.enrollmentPeriodId())) {
                    conn.rollback();
                    return new SectionChangeResult(false, "Target section belongs to a different enrollment period.");
                }

                if (hasEnrollmentDetailForOffering(conn, sourceSelection.enrollmentId(), targetOfferingId)) {
                    conn.rollback();
                    return new SectionChangeResult(false, "Student is already enrolled in the selected section.");
                }

                if (!hasAvailableSlot(conn, targetOfferingId)) {
                    conn.rollback();
                    return new SectionChangeResult(false, "The selected section is already full.");
                }

                String conflictMessage = validateScheduleConflict(
                    conn,
                    sourceSelection.enrollmentId(),
                    enrollmentDetailId,
                    targetOfferingId
                );
                if (conflictMessage != null) {
                    conn.rollback();
                    return new SectionChangeResult(false, conflictMessage);
                }

                insertEnrollmentDetail(conn, sourceSelection, targetOfferingId);
                migrateStudentEnrolledSubject(conn, studentId, sourceSelection, targetSelection);
                deleteEnrollmentDetail(conn, enrollmentDetailId);

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                logger.error("ERROR: {}", e.getMessage(), e);
                return new SectionChangeResult(false, "Failed to change section. Please try again.");
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("ERROR: {}", e.getMessage(), e);
            return new SectionChangeResult(false, "Failed to change section. Please try again.");
        }

        StudentSemesterProgressService.getInstance().syncByEnrollmentId(sourceSelection.enrollmentId());
        return new SectionChangeResult(true, "Section and schedule updated successfully.");
    }

    private Optional<EnrollmentSnapshot> getLatestEnrollmentSnapshot(String studentId) {
        String sql = """
            SELECT id, enrollment_period_id
            FROM enrollments
            WHERE student_id = ?
            ORDER BY created_at DESC
            FETCH FIRST 1 ROWS ONLY
            """;

        try (
            Connection conn = ConnectionService.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new EnrollmentSnapshot(rsGetLong(rs, "id"), rsGetLong(rs, "enrollment_period_id")));
                }
            }
        } catch (SQLException e) {
            logger.error("ERROR: {}", e.getMessage(), e);
        }

        return Optional.empty();
    }

    private Optional<SourceSelection> getSourceSelection(Connection conn, String studentId, Long enrollmentDetailId)
        throws SQLException {
        String sql = """
            SELECT
              ed.id,
              ed.enrollment_id,
              ed.offering_id,
              ed.units,
              ed.status,
              o.subject_id,
              o.enrollment_period_id
            FROM enrollments_details ed
            INNER JOIN enrollments e ON e.id = ed.enrollment_id
            INNER JOIN offerings o ON o.id = ed.offering_id
            WHERE ed.id = ?
              AND e.student_id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentDetailId);
            ps.setString(2, studentId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new SourceSelection(
                        rsGetLong(rs, "id"),
                        rsGetLong(rs, "enrollment_id"),
                        rsGetLong(rs, "offering_id"),
                        rsGetLong(rs, "subject_id"),
                        rsGetLong(rs, "enrollment_period_id"),
                        rs.getObject("units", Float.class),
                        safeText(rs.getString("status"), EnrollmentDetailStatus.SELECTED.name())
                    ));
                }
            }
        }

        return Optional.empty();
    }

    private Optional<TargetSelection> getTargetSelection(Connection conn, Long targetOfferingId) throws SQLException {
        String sql = """
            SELECT id, subject_id, enrollment_period_id, semester_subject_id
            FROM offerings
            WHERE id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, targetOfferingId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new TargetSelection(
                        rsGetLong(rs, "id"),
                        rsGetLong(rs, "subject_id"),
                        rsGetLong(rs, "enrollment_period_id"),
                        rs.getObject("semester_subject_id", Long.class)
                    ));
                }
            }
        }

        return Optional.empty();
    }

    private boolean hasEnrollmentDetailForOffering(Connection conn, Long enrollmentId, Long offeringId) throws SQLException {
        String sql = "SELECT 1 FROM enrollments_details WHERE enrollment_id = ? AND offering_id = ? FETCH FIRST 1 ROWS ONLY";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            ps.setLong(2, offeringId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private boolean hasAvailableSlot(Connection conn, Long offeringId) throws SQLException {
        Integer effectiveCapacity = getEffectiveCapacity(conn, offeringId);
        if (effectiveCapacity == null || effectiveCapacity <= 0) {
            return true;
        }

        String sql = "SELECT COUNT(*) AS total FROM enrollments_details WHERE offering_id = ? AND status = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, offeringId);
            ps.setString(2, EnrollmentDetailStatus.SELECTED.name());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total") < effectiveCapacity;
                }
            }
        }

        return true;
    }

    private Integer getEffectiveCapacity(Connection conn, Long offeringId) throws SQLException {
        String sql = """
            SELECT COALESCE(o.capacity, sec.capacity) AS effective_capacity
            FROM offerings o
            INNER JOIN sections sec ON sec.id = o.section_id
            WHERE o.id = ?
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, offeringId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getObject("effective_capacity", Integer.class);
                }
            }
        }

        return null;
    }

    private String validateScheduleConflict(
        Connection conn,
        Long enrollmentId,
        Long excludedEnrollmentDetailId,
        Long targetOfferingId
    ) throws SQLException {
        List<ScheduleSlot> targetSlots = getOfferingScheduleSlots(conn, targetOfferingId, "selected section");
        if (targetSlots.isEmpty()) {
            return null;
        }

        String sql = """
            SELECT
              sch.day,
              sch.start_time,
              sch.end_time,
              sub.subject_code,
              sec.section_code
            FROM enrollments_details ed
            INNER JOIN offerings o ON o.id = ed.offering_id
            INNER JOIN subjects sub ON sub.id = o.subject_id
            INNER JOIN sections sec ON sec.id = o.section_id
            INNER JOIN schedules sch ON sch.offering_id = o.id
            WHERE ed.enrollment_id = ?
              AND ed.status = ?
              AND ed.id <> ?
            """;

        List<ScheduleSlot> currentSlots = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentId);
            ps.setString(2, EnrollmentDetailStatus.SELECTED.name());
            ps.setLong(3, excludedEnrollmentDetailId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time startTime = rs.getTime("start_time");
                    Time endTime = rs.getTime("end_time");
                    String day = rs.getString("day");

                    if (day == null || startTime == null || endTime == null) {
                        continue;
                    }

                    String label = safeText(rs.getString("subject_code"), "Subject")
                        + " - " + safeText(rs.getString("section_code"), "Section");

                    currentSlots.add(new ScheduleSlot(day, startTime.toLocalTime(), endTime.toLocalTime(), label));
                }
            }
        }

        for (ScheduleSlot targetSlot : targetSlots) {
            for (ScheduleSlot currentSlot : currentSlots) {
                if (!targetSlot.day().equals(currentSlot.day())) {
                    continue;
                }

                if (isOverlapping(targetSlot.startTime(), targetSlot.endTime(), currentSlot.startTime(), currentSlot.endTime())) {
                    return "Schedule conflict with " + currentSlot.label() + ".";
                }
            }
        }

        return null;
    }

    private List<ScheduleSlot> getOfferingScheduleSlots(Connection conn, Long offeringId, String label) throws SQLException {
        String sql = "SELECT day, start_time, end_time FROM schedules WHERE offering_id = ?";
        List<ScheduleSlot> slots = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, offeringId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Time startTime = rs.getTime("start_time");
                    Time endTime = rs.getTime("end_time");
                    String day = rs.getString("day");

                    if (day == null || startTime == null || endTime == null) {
                        continue;
                    }

                    slots.add(new ScheduleSlot(day, startTime.toLocalTime(), endTime.toLocalTime(), label));
                }
            }
        }

        return slots;
    }

    private boolean isOverlapping(LocalTime startA, LocalTime endA, LocalTime startB, LocalTime endB) {
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    private void insertEnrollmentDetail(Connection conn, SourceSelection sourceSelection, Long targetOfferingId)
        throws SQLException {
        String sql = """
            INSERT INTO enrollments_details (enrollment_id, offering_id, units, status)
            VALUES (?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, sourceSelection.enrollmentId());
            ps.setLong(2, targetOfferingId);

            if (sourceSelection.units() == null) {
                ps.setNull(3, Types.FLOAT);
            } else {
                ps.setFloat(3, sourceSelection.units());
            }

            ps.setString(4, sourceSelection.status());
            ps.executeUpdate();
        }
    }

    private void migrateStudentEnrolledSubject(
        Connection conn,
        String studentId,
        SourceSelection sourceSelection,
        TargetSelection targetSelection
    ) throws SQLException {
        String selectSourceSql = """
            SELECT status
            FROM student_enrolled_subjects
            WHERE student_id = ?
              AND enrollment_id = ?
              AND offering_id = ?
            """;

        String deleteSourceSql = """
            DELETE FROM student_enrolled_subjects
            WHERE student_id = ?
              AND enrollment_id = ?
              AND offering_id = ?
            """;

        String updateSourceSql = """
            UPDATE student_enrolled_subjects
            SET offering_id = ?,
                semester_subject_id = ?,
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE student_id = ?
              AND enrollment_id = ?
              AND offering_id = ?
            """;

        String updateTargetSql = """
            UPDATE student_enrolled_subjects
            SET enrollment_id = ?,
                semester_subject_id = ?,
                status = ?,
                updated_at = CURRENT_TIMESTAMP
            WHERE student_id = ?
              AND offering_id = ?
            """;

        String targetExistsSql = "SELECT 1 FROM student_enrolled_subjects WHERE student_id = ? AND offering_id = ? FETCH FIRST 1 ROWS ONLY";

        String sourceStatus = null;
        try (PreparedStatement ps = conn.prepareStatement(selectSourceSql)) {
            ps.setString(1, studentId);
            ps.setLong(2, sourceSelection.enrollmentId());
            ps.setLong(3, sourceSelection.offeringId());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    sourceStatus = rs.getString("status");
                }
            }
        }

        if (sourceStatus == null) {
            return;
        }

        if (targetSelection.semesterSubjectId() == null) {
            try (PreparedStatement ps = conn.prepareStatement(deleteSourceSql)) {
                ps.setString(1, studentId);
                ps.setLong(2, sourceSelection.enrollmentId());
                ps.setLong(3, sourceSelection.offeringId());
                ps.executeUpdate();
            }
            return;
        }

        boolean targetExists;
        try (PreparedStatement ps = conn.prepareStatement(targetExistsSql)) {
            ps.setString(1, studentId);
            ps.setLong(2, targetSelection.offeringId());

            try (ResultSet rs = ps.executeQuery()) {
                targetExists = rs.next();
            }
        }

        if (targetExists) {
            try (PreparedStatement ps = conn.prepareStatement(updateTargetSql)) {
                ps.setLong(1, sourceSelection.enrollmentId());
                ps.setLong(2, targetSelection.semesterSubjectId());
                ps.setString(3, sourceStatus);
                ps.setString(4, studentId);
                ps.setLong(5, targetSelection.offeringId());
                ps.executeUpdate();
            }

            if (!sourceSelection.offeringId().equals(targetSelection.offeringId())) {
                try (PreparedStatement ps = conn.prepareStatement(deleteSourceSql)) {
                    ps.setString(1, studentId);
                    ps.setLong(2, sourceSelection.enrollmentId());
                    ps.setLong(3, sourceSelection.offeringId());
                    ps.executeUpdate();
                }
            }

            return;
        }

        try (PreparedStatement ps = conn.prepareStatement(updateSourceSql)) {
            ps.setLong(1, targetSelection.offeringId());
            ps.setLong(2, targetSelection.semesterSubjectId());
            ps.setString(3, sourceStatus);
            ps.setString(4, studentId);
            ps.setLong(5, sourceSelection.enrollmentId());
            ps.setLong(6, sourceSelection.offeringId());
            ps.executeUpdate();
        }
    }

    private void deleteEnrollmentDetail(Connection conn, Long enrollmentDetailId) throws SQLException {
        String sql = "DELETE FROM enrollments_details WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, enrollmentDetailId);
            ps.executeUpdate();
        }
    }

    private Map<Long, Integer> getSelectedCountByOffering(Connection conn, Set<Long> offeringIds) throws SQLException {
        if (offeringIds == null || offeringIds.isEmpty()) {
            return Map.of();
        }

        StringBuilder placeholders = new StringBuilder();
        for (int index = 0; index < offeringIds.size(); index++) {
            placeholders.append('?');
            if (index < offeringIds.size() - 1) {
                placeholders.append(',');
            }
        }

        String sql = "SELECT offering_id, COUNT(*) AS total FROM enrollments_details WHERE status = ? AND offering_id IN ("
            + placeholders + ") GROUP BY offering_id";

        Map<Long, Integer> selectedCountByOffering = new LinkedHashMap<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            int parameterIndex = 1;
            ps.setString(parameterIndex++, EnrollmentDetailStatus.SELECTED.name());
            for (Long offeringId : offeringIds) {
                ps.setLong(parameterIndex++, offeringId);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    selectedCountByOffering.put(rsGetLong(rs, "offering_id"), rs.getInt("total"));
                }
            }
        }

        return selectedCountByOffering;
    }

    private static Long rsGetLong(ResultSet rs, String column) {
        try {
            long value = rs.getLong(column);
            return rs.wasNull() ? null : value;
        } catch (SQLException e) {
            return null;
        }
    }

    private String buildFacultyDisplayName(String firstName, String lastName) {
        String safeLastName = lastName == null ? "" : lastName.trim();
        String safeFirstName = firstName == null ? "" : firstName.trim();

        if (safeLastName.isEmpty() && safeFirstName.isEmpty()) {
            return "";
        }

        if (safeLastName.isEmpty()) {
            return safeFirstName;
        }

        if (safeFirstName.isEmpty()) {
            return safeLastName;
        }

        return safeLastName + ", " + safeFirstName;
    }

    private String formatTime(Time time) {
        return time.toLocalTime().format(TIME_FORMATTER);
    }

    private static String safeText(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }

        return text.trim();
    }

    private void appendScheduleMetadata(
        ResultSet rs,
        Set<String> scheduleParts,
        Set<String> roomParts,
        Set<String> instructorParts
    ) throws SQLException {
        String day = rs.getString("day");
        Time startTime = rs.getTime("start_time");
        Time endTime = rs.getTime("end_time");
        if (day != null && startTime != null && endTime != null) {
            scheduleParts.add(day + " " + formatTime(startTime) + "-" + formatTime(endTime));
        }

        String roomName = rs.getString("room_name");
        if (roomName != null && !roomName.isBlank()) {
            roomParts.add(roomName.trim());
        }

        String facultyName = buildFacultyDisplayName(rs.getString("faculty_first_name"), rs.getString("faculty_last_name"));
        if (!facultyName.isBlank()) {
            instructorParts.add(facultyName);
        }
    }

}
