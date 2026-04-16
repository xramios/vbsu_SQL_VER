package com.group5.paul_esys.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group5.paul_esys.modules.enums.EnrollmentDetailStatus;
import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.enrollments.model.EnrollmentDetail;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentDetailService;
import com.group5.paul_esys.modules.enrollments.services.EnrollmentService;
import com.group5.paul_esys.modules.enrollments.services.StudentAcademicPromotionService;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrollmentEligibilityService;
import com.group5.paul_esys.modules.enrollments.services.StudentEnrolledSubjectService;
import com.group5.paul_esys.modules.enrollments.services.StudentSemesterProgressService;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.model.StudentStatus;
import com.group5.paul_esys.modules.students.services.StudentService;
import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.group5.paul_esys.testsupport.ServiceTestSupport;

class EnrollmentServicesTest extends ServiceTestSupport {

  @Test
  void studentServiceReturnsEmptyOptionalWhenStudentMissing() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentService.getInstance().get("20250001").isEmpty()));
  }

  @Test
  void studentServiceInsertSucceedsWhenRowsAreInserted() throws Exception {
    Student student = new Student()
        .setStudentId("20250001")
        .setUserId(1L)
        .setFirstName("Ada")
        .setLastName("Lovelace")
        .setMiddleName("B")
        .setBirthdate(Date.valueOf(LocalDate.of(2000, 1, 1)))
        .setStudentStatus(StudentStatus.REGULAR)
        .setCourseId(1L)
        .setCurriculumId(1L)
        .setYearLevel(1L);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(StudentService.getInstance().insert(student).isPresent()));
  }

  @Test
  void studentServiceUpdateSucceedsWhenRowsAreUpdated() throws Exception {
    Student student = new Student()
        .setStudentId("20250001")
        .setUserId(1L)
        .setFirstName("Ada")
        .setLastName("Lovelace")
        .setMiddleName("B")
        .setBirthdate(Date.valueOf(LocalDate.of(2000, 1, 1)))
        .setStudentStatus(StudentStatus.REGULAR)
        .setCourseId(1L)
        .setCurriculumId(1L)
        .setYearLevel(2L);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(StudentService.getInstance().update(student).isPresent()));
  }

  @Test
  void studentServiceRejectsBlankAvailabilityChecks() {
    assertFalse(StudentService.getInstance().isEmailAvailable(" "));
    assertFalse(StudentService.getInstance().isStudentIdAvailable(null));
  }

  @Test
  void studentServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentService.getInstance().list().isEmpty()));
  }

  @Test
  void enrollmentServiceRejectsNullEnrollment() {
    assertFalse(EnrollmentService.getInstance().createEnrollment(null));
  }

  @Test
  void enrollmentServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(EnrollmentService.getInstance().getAllEnrollments().isEmpty()));
  }

  @Test
  void enrollmentServiceUpdateStatusSucceedsWithoutAStudentLink() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(EnrollmentService.getInstance().updateEnrollmentStatus(1L, EnrollmentStatus.APPROVED)));
  }

  @Test
  void enrollmentServiceDeleteSucceedsWithoutAStudentLink() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(EnrollmentService.getInstance().deleteEnrollment(1L)));
  }

  @Test
  void enrollmentServiceBackfillReturnsZeroWhenNoRowsAreUpdated() throws Exception {
    withConnection(mockUpdateConnectionOnly(0), () ->
        assertEquals(0, EnrollmentService.getInstance().backfillCompletedEnrollments()));
  }

  @Test
  void enrollmentDetailServiceRejectsIncompleteCreateRequest() {
    EnrollmentDetail detail = new EnrollmentDetail()
        .setEnrollmentId(null)
        .setOfferingId(null)
        .setUnits(3.0f)
        .setStatus(EnrollmentDetailStatus.SELECTED);

    assertFalse(EnrollmentDetailService.getInstance().createEnrollmentDetail(detail));
  }

  @Test
  void enrollmentDetailServiceReturnsZeroWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertEquals(0L, EnrollmentDetailService.getInstance().countSelectedByOffering(1L)));
  }

  @Test
  void enrollmentDetailServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(EnrollmentDetailService.getInstance().getEnrollmentDetailsByEnrollment(1L).isEmpty()));
  }

  @Test
  void studentEnrolledSubjectServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentEnrolledSubjectService.getInstance().getByStudent("20250001").isEmpty()));
  }

  @Test
  void studentEnrolledSubjectServiceReturnsEmptyOptionalWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentEnrolledSubjectService.getInstance().getByStudentAndOffering("20250001", 1L).isEmpty()));
  }

  @Test
  void studentEnrolledSubjectServiceRejectsInvalidDeleteRequest() {
    assertFalse(StudentEnrolledSubjectService.getInstance().delete(" ", 1L));
  }

  @Test
  void studentSemesterProgressServiceRejectsBlankStudentId() {
    assertFalse(StudentSemesterProgressService.getInstance().syncStudentProgress(" "));
  }

  @Test
  void studentSemesterProgressServiceRejectsInvalidInitialization() throws Exception {
    assertFalse(StudentSemesterProgressService.getInstance().initializeInitialSemesterProgress(null, null, null));
  }

  @Test
  void studentSemesterProgressServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(StudentSemesterProgressService.getInstance().getProgressByStudent("20250001").isEmpty()));
  }

  @Test
  void studentAcademicPromotionServiceRejectsInvalidInput() throws Exception {
    assertFalse(StudentAcademicPromotionService.getInstance().promoteIfYearCompleted(null, "20250001"));
    var jdbc = mockEmptyQueryConnection();
    withConnection(jdbc.connection(), () ->
        assertFalse(StudentAcademicPromotionService.getInstance().promoteIfYearCompleted(jdbc.connection(), "20250001")));
  }

  @Test
  void studentEnrollmentEligibilityServiceRejectsBlankStudentId() {
    assertTrue(StudentEnrollmentEligibilityService.getInstance().getEligibleSemesterSubjectIds(" ", null, null).isEmpty());
    assertEquals(0.0f, StudentEnrollmentEligibilityService.getInstance().getCurrentSemesterUnitLimit(" ", null, null));
  }
}