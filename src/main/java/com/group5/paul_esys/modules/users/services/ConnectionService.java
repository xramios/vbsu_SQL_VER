package com.group5.paul_esys.modules.users.services;

import com.group5.paul_esys.modules.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    /**
     *
     * Gagawa siya ng connection sa database.
     *
     * @return - Yung connection pero if nagfail magrreturn ng null
     */
    public static Connection getConnection() {
        try {
            Class.forName("org.apache.derby.client.ClientAutoloadedDriver");
            Class.forName("com.mysql.cj.jdbc.Driver");
            return DriverManager.getConnection(Config.DB_URL, Config.DB_USER, Config.DB_PASS);
        } catch (SQLException e) {
            logger.error("ERROR: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Error connecting to database: " + e.getMessage());
            System.exit(1);
        } catch (ClassNotFoundException e) {
            logger.error("ERROR: " + e.getMessage(), e);
            JOptionPane.showMessageDialog(null, "Error loading Apache Derby JDBC Driver: " + e.getMessage());
            System.exit(1);
        }

        return null;
    }
}
