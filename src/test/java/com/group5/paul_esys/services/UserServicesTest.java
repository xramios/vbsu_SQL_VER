package com.group5.paul_esys.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.group5.paul_esys.modules.students.model.Student;
import com.group5.paul_esys.modules.students.model.StudentStatus;
import com.group5.paul_esys.modules.users.models.enums.Role;
import com.group5.paul_esys.modules.users.models.user.LoginData;
import com.group5.paul_esys.modules.users.models.user.UserInformation;
import com.group5.paul_esys.modules.users.services.AccountSecurityService;
import com.group5.paul_esys.modules.users.services.AuthService;
import com.group5.paul_esys.modules.users.services.PasswordResetService;
import com.group5.paul_esys.modules.users.services.UserDirectoryService;
import com.group5.paul_esys.modules.users.services.UserSession;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import com.group5.paul_esys.testsupport.ServiceTestSupport;
import static org.mockito.Mockito.when;

class UserServicesTest extends ServiceTestSupport {

  @Test
  void accountSecurityServiceVerifiesMatchingPassword() throws Exception {
    String plainPassword = "Secret123!";
    String hashedPassword = BCrypt.withDefaults().hashToString(12, plainPassword.toCharArray());

    var jdbc = mockEmptyQueryConnection();
    when(jdbc.resultSet().next()).thenReturn(true, false);
    when(jdbc.resultSet().getString("password")).thenReturn(hashedPassword);

    withConnection(jdbc.connection(), () ->
        assertTrue(AccountSecurityService.getInstance().verifyCurrentPassword(1L, plainPassword)));
  }

  @Test
  void accountSecurityServiceUpdatePasswordSucceeds() throws Exception {
    withConnection(mockUpdateConnectionOnly(1), () ->
        assertTrue(AccountSecurityService.getInstance().updatePassword(1L, "NewSecret123!")));
  }

  @Test
  void authServiceRejectsInvalidLoginData() {
    assertThrows(IllegalArgumentException.class, () -> AuthService.getInstance().login(new LoginData(null, null)));
  }

  @Test
  void authServiceReturnsEmptyWhenUserNotFound() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () -> {
      Optional<?> result = AuthService.getInstance().login(new LoginData("missing@example.com", "Password123!"));
      assertTrue(result.isEmpty());
    });
  }

  @Test
  void passwordResetServiceReturnsNullWhenUserIsMissing() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
      assertTrue(new PasswordResetService().generateResetToken("missing@example.com") == null));
  }

  @Test
  void passwordResetServiceRejectsMissingToken() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertFalse(new PasswordResetService().validateToken("missing@example.com", "123456")));
  }

  @Test
  void passwordResetServiceResetPasswordFailsWhenTokenIsMissing() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertFalse(new PasswordResetService().resetPassword("missing@example.com", "123456", "NewPass123!")));
  }

  @Test
  void userDirectoryServiceReturnsEmptyListWhenNoUsersExist() throws Exception {
    withConnection(mockEmptyQueryConnectionOnly(), () ->
        assertTrue(UserDirectoryService.getInstance().getAllUsers().isEmpty()));
  }

  @Test
  void userDirectoryServiceRejectsNullUser() {
    assertFalse(UserDirectoryService.getInstance().deleteUser(null));
  }

  @Test
  void userSessionStoresAndClearsInformation() {
    UserSession session = UserSession.getInstance();
    UserInformation<Student> userInformation = new UserInformation<Student>(1L, "student@example.com", "secret", Role.STUDENT)
        .setUser(new Student()
            .setStudentId("20250001")
            .setUserId(1L)
            .setFirstName("Ada")
            .setLastName("Lovelace")
            .setBirthdate(Date.valueOf(LocalDate.of(2000, 1, 1)))
            .setStudentStatus(StudentStatus.REGULAR)
            .setCourseId(1L)
            .setCurriculumId(1L)
            .setYearLevel(1L));

    session.setUserInformation(userInformation);
    assertNotNull(session.getUserInformation());
    session.logout();
    assertTrue(UserSession.getInstance().getUserInformation() == null);
  }
}