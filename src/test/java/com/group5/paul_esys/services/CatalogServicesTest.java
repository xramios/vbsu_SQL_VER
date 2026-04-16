package com.group5.paul_esys.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group5.paul_esys.modules.courses.services.CourseService;
import com.group5.paul_esys.modules.curriculum.services.CurriculumService;
import com.group5.paul_esys.modules.departments.model.Department;
import com.group5.paul_esys.modules.departments.services.DepartmentService;
import com.group5.paul_esys.modules.enums.DayOfWeek;
import com.group5.paul_esys.modules.offerings.model.Offering;
import com.group5.paul_esys.modules.offerings.services.OfferingGenerationService;
import com.group5.paul_esys.modules.offerings.services.OfferingService;
import com.group5.paul_esys.modules.prerequisites.services.PrerequisiteService;
import com.group5.paul_esys.modules.rooms.services.RoomService;
import com.group5.paul_esys.modules.schedules.model.Schedule;
import com.group5.paul_esys.modules.schedules.services.ScheduleService;
import com.group5.paul_esys.modules.sections.services.SectionService;
import com.group5.paul_esys.modules.semester.services.SemesterService;
import com.group5.paul_esys.modules.semester_subjects.services.SemesterSubjectService;
import com.group5.paul_esys.modules.subjects.services.SubjectService;
import java.sql.Time;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import com.group5.paul_esys.testsupport.ServiceTestSupport;

class CatalogServicesTest extends ServiceTestSupport {

  @Test
  void departmentServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    var jdbc = mockEmptyQueryConnection();
    withConnection(jdbc.connection(), () ->
        assertTrue(DepartmentService.getInstance().getAllDepartments().isEmpty()));
  }

  @Test
  void departmentServiceCreateSucceedsOnUpdate() throws Exception {
    Department department = new Department()
        .setDepartmentName("Engineering")
        .setDepartmentCode("ENG")
        .setDescription("Engineering department");

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(DepartmentService.getInstance().createDepartment(department)));
  }

  @Test
  void courseServiceReturnsEmptyOptionalWhenNotFound() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(CourseService.getInstance().getCourseById(1L).isEmpty()));
  }

  @Test
  void courseServiceDeleteSucceedsOnUpdate() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(CourseService.getInstance().deleteCourse(1L)));
  }

  @Test
  void roomServiceReturnsEmptyBuildingsWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(RoomService.getInstance().getDistinctBuildings().isEmpty()));
  }

  @Test
  void roomServiceDeleteSucceedsOnUpdate() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(RoomService.getInstance().deleteRoom(1L)));
  }

  @Test
  void sectionServiceReturnsZeroWhenNoReservedStudentsExist() {
    assertEquals(0L, SectionService.getInstance().countReservedStudentsBySection(null, 1L, null));
  }

  @Test
  void sectionServiceReturnsEmptySelectedCountMapWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(SectionService.getInstance().getSelectedEnrollmentCountBySectionId().isEmpty()));
  }

  @Test
  void subjectServiceReturnsEmptyOptionalWhenNotFound() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(SubjectService.getInstance().getSubjectByCode("MATH101").isEmpty()));
  }

  @Test
  void subjectServiceDeleteSucceedsOnUpdate() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(SubjectService.getInstance().deleteSubject(1L)));
  }

  @Test
  void offeringServiceCreateSucceedsOnUpdate() throws Exception {
    Offering offering = new Offering()
        .setSubjectId(1L)
        .setSectionId(2L)
        .setEnrollmentPeriodId(3L)
        .setSemesterSubjectId(4L)
        .setCapacity(40);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(OfferingService.getInstance().createOffering(offering)));
  }

  @Test
  void offeringServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(OfferingService.getInstance().getOfferingsBySection(2L).isEmpty()));
  }

  @Test
  void offeringServiceExistsOfferingReturnsFalseWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertFalse(OfferingService.getInstance().existsOffering(1L, 2L, 3L)));
  }

  @Test
  void semesterSubjectServiceReturnsEmptyOptionalWhenNotFound() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(SemesterSubjectService.getInstance().getBySemesterAndSubject(1L, 2L).isEmpty()));
  }

  @Test
  void semesterSubjectServiceDeleteBySubjectsSucceedsOnUpdate() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(SemesterSubjectService.getInstance().deleteBySemesterAndSubject(1L, 2L)));
  }

  @Test
  void prerequisiteServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(PrerequisiteService.getInstance().getPrerequisitesBySubject(1L).isEmpty()));
  }

  @Test
  void prerequisiteServiceDeleteBySubjectsSucceedsOnUpdate() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(PrerequisiteService.getInstance().deletePrerequisiteBySubjects(1L, 2L)));
  }

  @Test
  void scheduleServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(ScheduleService.getInstance().getSchedulesByDay(DayOfWeek.MON).isEmpty()));
  }

  @Test
  void scheduleServiceCreateSucceedsOnUpdate() throws Exception {
    Schedule schedule = new Schedule()
        .setOfferingId(1L)
        .setRoomId(2L)
        .setFacultyId(3L)
        .setDay(DayOfWeek.MON)
        .setStartTime(Time.valueOf(LocalTime.of(8, 0)))
        .setEndTime(Time.valueOf(LocalTime.of(9, 30)));

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(ScheduleService.getInstance().createSchedule(schedule)));
  }

  @Test
  void curriculumServiceReturnsZeroUnitsWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertEquals(0.0f, CurriculumService.getInstance().getTotalUnitsForSemester(1L, "First Semester")));
  }

  @Test
  void semesterServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(SemesterService.getInstance().getAllSemesters().isEmpty()));
  }

  @Test
  void semesterServiceRejectsNullSemesterChecks() {
    assertFalse(SemesterService.getInstance().semesterExists(null));
  }

  @Test
  void offeringGenerationServiceRejectsMissingEnrollmentPeriod() {
    var result = OfferingGenerationService.getInstance().generateOfferings(
        null,
        null,
        null,
        null,
        false,
        false
    );

    assertTrue(result.successful());
    assertEquals(0, result.candidateCount());
    assertEquals(0, result.createdCount());
    assertEquals("No matching subject-section combinations were found for the selected filters.", result.message());
  }
}