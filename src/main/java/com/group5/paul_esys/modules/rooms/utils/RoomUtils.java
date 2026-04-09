package com.group5.paul_esys.modules.rooms.utils;

import com.group5.paul_esys.modules.rooms.model.Room;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

public class RoomUtils {

  public static Room mapResultSetToRoom(ResultSet rs) throws SQLException {
    Integer capacity = rs.getInt("capacity");
    if (rs.wasNull()) {
      capacity = null;
    }

    return new Room(
        rs.getLong("id"),
        getOptionalColumnValue(rs, "building", "N/A"),
        getOptionalColumnValue(rs, "room_type", "OTHER"),
        getOptionalColumnValue(rs, "status", "AVAILABLE"),
        rs.getString("room"),
        capacity
    );
  }

  private static String getOptionalColumnValue(ResultSet rs, String columnName, String fallback) throws SQLException {
    if (!hasColumn(rs, columnName)) {
      return fallback;
    }

    String value = rs.getString(columnName);
    return value == null || value.trim().isEmpty() ? fallback : value.trim();
  }

  private static boolean hasColumn(ResultSet rs, String columnName) throws SQLException {
    ResultSetMetaData metadata = rs.getMetaData();
    for (int index = 1; index <= metadata.getColumnCount(); index++) {
      String label = metadata.getColumnLabel(index);
      String name = metadata.getColumnName(index);

      if (columnName.equalsIgnoreCase(label) || columnName.equalsIgnoreCase(name)) {
        return true;
      }
    }

    return false;
  }
}
