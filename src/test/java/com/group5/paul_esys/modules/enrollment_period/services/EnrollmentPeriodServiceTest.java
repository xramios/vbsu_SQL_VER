package com.group5.paul_esys.modules.enrollment_period.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.group5.paul_esys.modules.enrollment_period.model.EnrollmentPeriod;
import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.group5.paul_esys.testsupport.ServiceTestSupport;

class EnrollmentPeriodServiceTest extends ServiceTestSupport {

  private final EnrollmentPeriodService service = EnrollmentPeriodService.getInstance();

  @Test
  void createEnrollmentPeriodRejectsWhenAnotherPeriodIsAlreadyOpen() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement openCheckStatement = mock(PreparedStatement.class);
    ResultSet openPeriodResultSet = mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenAnswer(invocation -> openCheckStatement);
    when(openCheckStatement.executeQuery()).thenReturn(openPeriodResultSet);
    when(openPeriodResultSet.next()).thenReturn(true);
    stubEnrollmentPeriodRow(openPeriodResultSet, 1L, "2025-2026", "First Semester");

    EnrollmentPeriod period = buildPeriod("2026-2027", "Second Semester", LocalDate.now().plusDays(10), LocalDate.now().plusDays(20));

    try (MockedStatic<ConnectionService> mockedConnectionService = mockStatic(ConnectionService.class)) {
      mockedConnectionService.when(ConnectionService::getConnection).thenReturn(connection);

      assertFalse(service.createEnrollmentPeriod(period));
    }
  }

  @Test
  void updateEnrollmentPeriodRejectsWhenAnotherPeriodIsAlreadyOpen() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement openCheckStatement = mock(PreparedStatement.class);
    ResultSet openPeriodResultSet = mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenAnswer(invocation -> openCheckStatement);
    when(openCheckStatement.executeQuery()).thenReturn(openPeriodResultSet);
    when(openPeriodResultSet.next()).thenReturn(true);
    stubEnrollmentPeriodRow(openPeriodResultSet, 99L, "2025-2026", "First Semester");

    EnrollmentPeriod period = buildPeriod("2024-2025", "Summer", LocalDate.now().plusDays(1), LocalDate.now().plusDays(5));
    period.setId(10L);

    try (MockedStatic<ConnectionService> mockedConnectionService = mockStatic(ConnectionService.class)) {
      mockedConnectionService.when(ConnectionService::getConnection).thenReturn(connection);

      assertFalse(service.updateEnrollmentPeriod(period));
    }
  }

  @Test
  void deleteEnrollmentPeriodRejectsWhenDeletingTheOpenPeriod() throws Exception {
    Connection connection = mock(Connection.class);
    PreparedStatement openCheckStatement = mock(PreparedStatement.class);
    ResultSet openPeriodResultSet = mock(ResultSet.class);

    when(connection.prepareStatement(anyString())).thenAnswer(invocation -> openCheckStatement);
    when(openCheckStatement.executeQuery()).thenReturn(openPeriodResultSet);
    when(openPeriodResultSet.next()).thenReturn(true);
    stubEnrollmentPeriodRow(openPeriodResultSet, 15L, "2025-2026", "First Semester");

    try (MockedStatic<ConnectionService> mockedConnectionService = mockStatic(ConnectionService.class)) {
      mockedConnectionService.when(ConnectionService::getConnection).thenReturn(connection);

      assertFalse(service.deleteEnrollmentPeriod(15L));
    }
  }

  @Test
  void getAllEnrollmentPeriodsReturnsEmptyListWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(service.getAllEnrollmentPeriods().isEmpty()));
  }

  @Test
  void getEnrollmentPeriodByIdReturnsEmptyWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(service.getEnrollmentPeriodById(1L).isEmpty()));
  }

  @Test
  void getCurrentOrLatestEnrollmentPeriodReturnsEmptyWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(service.getCurrentOrLatestEnrollmentPeriod().isEmpty()));
  }

  @Test
  void getOpenEnrollmentPeriodExcludingReturnsEmptyWhenNoRowsExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(service.getOpenEnrollmentPeriodExcluding(1L).isEmpty()));
  }

  private EnrollmentPeriod buildPeriod(String schoolYear, String semester, LocalDate startDate, LocalDate endDate) {
    return new EnrollmentPeriod()
        .setSchoolYear(schoolYear)
        .setSemester(semester)
        .setStartDate(Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()))
      .setEndDate(Date.from(endDate.atTime(LocalTime.of(23, 59)).atZone(ZoneId.systemDefault()).toInstant()));
  }

  private void stubEnrollmentPeriodRow(ResultSet resultSet, long id, String schoolYear, String semester) throws Exception {
    when(resultSet.getLong("id")).thenReturn(id);
    when(resultSet.getString("school_year")).thenReturn(schoolYear);
    when(resultSet.getString("semester")).thenReturn(semester);
    when(resultSet.getTimestamp("start_date")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusDays(1)));
    when(resultSet.getTimestamp("end_date")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusDays(1)));
    when(resultSet.getString("description")).thenReturn("Open enrollment period");
    when(resultSet.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusHours(1)));
    when(resultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(LocalDateTime.now().minusDays(2)));
  }
}