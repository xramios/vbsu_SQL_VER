package com.group5.paul_esys.modules.users.services;

import com.group5.paul_esys.modules.users.models.user.UserInformation;
import com.group5.paul_esys.modules.users.models.enums.RemovalActionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;

/**
 * Service to log sensitive removal actions for audit purposes.
 * In a real-world scenario, this would persist to a database table.
 */
public class RemovalAuditService {
    private static final Logger logger = LoggerFactory.getLogger(RemovalAuditService.class);
    private static RemovalAuditService instance;

    private RemovalAuditService() {}

    public static synchronized RemovalAuditService getInstance() {
        if (instance == null) {
            instance = new RemovalAuditService();
        }
        return instance;
    }

    /**
     * Logs a removal action to the audit trail.
     * 
     * @param user The user performing the action
     * @param actionType The type of removal action
     * @param details Additional details (e.g., student ID, number of items removed)
     */
    public void logRemoval(Object user, RemovalActionType actionType, String details) {
        String username = "Unknown";
        if (user instanceof UserInformation<?> userInfo) {
            username = userInfo.getEmail();
        } else if (user != null) {
            username = user.toString();
        }
        
        String timestamp = LocalDateTime.now().toString();
        
        // In this implementation, we log to the application log.
        // If a database table is added, persist here.
        logger.warn("AUDIT_REMOVAL | User: {} | Action: {} | Timestamp: {} | Details: {}", 
                   username, actionType, timestamp, details);
        
        System.out.println("[AUDIT LOG] " + actionType + " by " + username + ": " + details);
    }
}
