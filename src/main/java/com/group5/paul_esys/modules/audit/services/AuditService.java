package com.group5.paul_esys.modules.audit.services;

import com.group5.paul_esys.modules.users.services.ConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class AuditService {
    private static final AuditService INSTANCE = new AuditService();
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    private AuditService() {}

    public static AuditService getInstance() {
        return INSTANCE;
    }

    public void logAction(String userId, String action, String details) {
        String sql = "INSERT INTO audit_logs (user_id, action, details, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = ConnectionService.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
            ps.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to log audit action: {}", e.getMessage(), e);
        }
    }
}
