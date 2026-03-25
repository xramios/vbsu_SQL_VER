package com.group5.paul_esys.modules.rooms.utils;

import com.group5.paul_esys.modules.rooms.model.Room;

import java.sql.ResultSet;
import java.sql.SQLException;

public class RoomUtils {

  public static Room mapResultSetToRoom(ResultSet rs) throws SQLException {
    return new Room(
        rs.getLong("id"),
        rs.getString("room"),
        rs.getInt("capacity")
    );
  }
}
