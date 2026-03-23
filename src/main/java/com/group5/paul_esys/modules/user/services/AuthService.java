package com.group5.paul_esys.modules.user.services;

import com.group5.paul_esys.modules.user.models.user.LoginData;
import com.group5.paul_esys.modules.user.models.user.UserInformation;
import com.group5.paul_esys.modules.user.utils.UserUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Optional;

public class AuthService {

  public Connection conn = ConnectionService.getConnection();

  public Optional<UserInformation> login(LoginData loginData) {
    if (!loginData.isValid()) {
      throw new IllegalArgumentException("Invalid login data");
    }

    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE email = ? AND password = ?");

      return Optional.of(
          UserUtils.mapResultSetToUserInformation(ps.executeQuery())
      );
    } catch (SQLException e) {
      System.out.println("ERROR: " + e.getMessage());
    }

    return Optional.empty();
  }
}
