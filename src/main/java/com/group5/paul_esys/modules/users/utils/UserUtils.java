package com.group5.paul_esys.modules.users.utils;

import com.group5.paul_esys.modules.users.services.ConnectionService;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shared user helper utilities.
 */
public final class UserUtils {
  private static final Logger logger = LoggerFactory.getLogger(UserUtils.class);

  private UserUtils() {}

  /**
   * Return the user's email for the given user id.
   *
   * This centralizes the common SQL lookup used across multiple services.
   */
  public static Optional<String> getUserEmailByUserId(Long userId) {
    if (userId == null) {
      return Optional.empty();
    }

    try (
        Connection conn = ConnectionService.getConnection();
        PreparedStatement ps = conn.prepareStatement("SELECT email FROM users WHERE id = ?")
    ) {
      ps.setLong(1, userId);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.ofNullable(rs.getString("email"));
        }
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }

    return Optional.empty();
  }
}
