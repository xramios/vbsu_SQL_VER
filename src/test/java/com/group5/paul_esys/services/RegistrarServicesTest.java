package com.group5.paul_esys.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.group5.paul_esys.modules.admin.model.Admin;
import com.group5.paul_esys.modules.admin.services.AdminService;
import com.group5.paul_esys.modules.enums.DropRequestStatus;
import com.group5.paul_esys.modules.enums.EnrollmentStatus;
import com.group5.paul_esys.modules.faculty.model.Faculty;
import com.group5.paul_esys.modules.faculty.services.FacultyClassListService;
import com.group5.paul_esys.modules.faculty.services.FacultyService;
import com.group5.paul_esys.modules.registrar.model.Registrar;
import com.group5.paul_esys.modules.registrar.model.ScheduleGenerationRequest;
import com.group5.paul_esys.modules.registrar.model.SectionChangeResult;
import com.group5.paul_esys.modules.registrar.services.RegistrarDropRequestService;
import com.group5.paul_esys.modules.registrar.services.RegistrarEnrollmentService;
import com.group5.paul_esys.modules.registrar.services.RegistrarScheduleManagementService;
import com.group5.paul_esys.modules.registrar.services.RegistrarService;
import com.group5.paul_esys.modules.registrar.services.RegistrarStudentScheduleService;
import java.sql.Date;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.group5.paul_esys.testsupport.ServiceTestSupport;

class RegistrarServicesTest extends ServiceTestSupport {

  @Test
  void registrarServiceReturnsEmptyOptionalWhenMissing() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(RegistrarService.getInstance().getRegistrarByEmployeeId("REG-001").isEmpty()));
  }

  @Test
  void registrarServiceCreateSucceedsOnUpdate() throws Exception {
    Registrar registrar = new Registrar()
        .setEmployeeId("REG-001")
        .setFirstName("Rita")
        .setLastName("Santos")
        .setContactNumber("09170000000");

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(RegistrarService.getInstance().createRegistrar(registrar)));
  }

  @Test
  void adminServiceCreateSucceedsOnUpdate() throws Exception {
    Admin admin = new Admin()
        .setUserId(1L)
        .setFirstName("Alice")
        .setLastName("Rivera")
        .setContactNumber("09170000001");

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(AdminService.getInstance().createAdmin(admin)));
  }

  @Test
  void facultyServiceCreateSucceedsOnUpdate() throws Exception {
    Faculty faculty = new Faculty()
        .setUserId(1L)
        .setFirstName("Bea")
        .setLastName("Cruz")
        .setMiddleName("L")
        .setContactNumber("09170000002")
        .setBirthdate(Date.valueOf(LocalDate.of(1990, 1, 1)))
        .setDepartmentId(1L);

    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(FacultyService.getInstance().createFaculty(faculty)));
  }

  @Test
  void registrarEnrollmentServiceReturnsEmptyResultsWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () -> {
      assertTrue(RegistrarEnrollmentService.getInstance().getEnrollmentApplications().isEmpty());
      assertEquals(0, RegistrarEnrollmentService.getInstance().countPendingActions());
      assertEquals(0, RegistrarEnrollmentService.getInstance().countTotalRegistered());
    });
  }

  @Test
  void registrarEnrollmentServiceNoOpsOnEmptyIdList() {
    assertEquals(0, RegistrarEnrollmentService.getInstance().updateApplicationsStatus(java.util.List.of(), EnrollmentStatus.APPROVED));
  }

  @Test
  void registrarDropRequestServiceRejectsInvalidRequests() {
    assertFalse(RegistrarDropRequestService.getInstance().approveDropRequest(null));
    assertFalse(RegistrarDropRequestService.getInstance().createDropRequest(null, null, null, null));
    assertFalse(RegistrarDropRequestService.getInstance().rejectDropRequest(null));
  }

  @Test
  void registrarDropRequestServiceReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(RegistrarDropRequestService.getInstance().getDropRequests(DropRequestStatus.PENDING).isEmpty()));
  }

  @Test
  void registrarStudentScheduleServiceReturnsEmptyResultsWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () -> {
      assertTrue(RegistrarStudentScheduleService.getInstance().getStudentSchedules("20250001").isEmpty());
      assertTrue(RegistrarStudentScheduleService.getInstance().getAvailableSectionSchedules(1L, 1L, null).isEmpty());
    });
  }

  @Test
  void registrarStudentScheduleServiceRejectsInvalidChangeRequest() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () -> {
      SectionChangeResult result = RegistrarStudentScheduleService.getInstance().changeStudentSection("20250001", 1L, 2L);
      assertFalse(result.success());
      assertTrue(result.message().contains("could not be found"));
    });
  }

  @Test
  void registrarScheduleManagementServiceReturnsEmptyListsWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () -> {
      assertTrue(RegistrarScheduleManagementService.getInstance().getScheduleRows().isEmpty());
      assertTrue(RegistrarScheduleManagementService.getInstance().getEnrollmentPeriodOptions().isEmpty());
    });
  }

  @Test
  void registrarScheduleManagementServiceRejectsNullGenerationRequest() {
    ScheduleGenerationRequest request = new ScheduleGenerationRequest(null, null, null, null, null);
    var preview = RegistrarScheduleManagementService.getInstance().previewSectionScheduleGeneration(request);
    var generated = RegistrarScheduleManagementService.getInstance().generateSectionSchedules(request);

    assertFalse(preview.successful());
    assertTrue(preview.rows().isEmpty());
    assertFalse(generated.successful());
    assertTrue(generated.rows().isEmpty());
  }

  @Test
  void facultyClassListServiceReturnsEmptyResultsWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () -> {
      assertTrue(FacultyClassListService.getInstance().getClassListRowsByFaculty(1L).isEmpty());
      assertTrue(FacultyClassListService.getInstance().getClassStudentsByOffering(1L).isEmpty());
    });
  }
}