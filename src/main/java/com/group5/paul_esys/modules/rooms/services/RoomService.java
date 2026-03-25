package com.group5.paul_esys.modules.rooms.services;

import com.group5.paul_esys.modules.rooms.model.Room;
import com.group5.paul_esys.modules.rooms.utils.RoomUtils;
import com.group5.paul_esys.modules.users.services.ConnectionService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoomService {

  private final Connection conn = ConnectionService.getConnection();
  private final Logger logger = LoggerFactory.getLogger(RoomService.class);

  public List<Room> getAllRooms() {
    List<Room> rooms = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms ORDER BY room");
      ResultSet rs = ps.executeQuery();
      
      while (rs.next()) {
        rooms.add(RoomUtils.mapResultSetToRoom(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return rooms;
  }

  public Optional<Room> getRoomById(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms WHERE id = ?");
      ps.setLong(1, id);
      
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return Optional.of(RoomUtils.mapResultSetToRoom(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public Optional<Room> getRoomByName(String roomName) {
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms WHERE room = ?");
      ps.setString(1, roomName);
      
      ResultSet rs = ps.executeQuery();
      if (rs.next()) {
        return Optional.of(RoomUtils.mapResultSetToRoom(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return Optional.empty();
  }

  public List<Room> getRoomsByCapacity(int minCapacity) {
    List<Room> rooms = new ArrayList<>();
    try {
      PreparedStatement ps = conn.prepareStatement("SELECT * FROM rooms WHERE capacity >= ? ORDER BY room");
      ps.setInt(1, minCapacity);
      
      ResultSet rs = ps.executeQuery();
      while (rs.next()) {
        rooms.add(RoomUtils.mapResultSetToRoom(rs));
      }
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
    }
    return rooms;
  }

  public boolean createRoom(Room room) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "INSERT INTO rooms (room, capacity) VALUES (?, ?)"
      );
      ps.setString(1, room.getRoom());
      ps.setInt(2, room.getCapacity());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean updateRoom(Room room) {
    try {
      PreparedStatement ps = conn.prepareStatement(
          "UPDATE rooms SET room = ?, capacity = ? WHERE id = ?"
      );
      ps.setString(1, room.getRoom());
      ps.setInt(2, room.getCapacity());
      ps.setLong(3, room.getId());
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }

  public boolean deleteRoom(Long id) {
    try {
      PreparedStatement ps = conn.prepareStatement("DELETE FROM rooms WHERE id = ?");
      ps.setLong(1, id);
      
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      logger.error("ERROR: " + e.getMessage(), e);
      return false;
    }
  }
}
