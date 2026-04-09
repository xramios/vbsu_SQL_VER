package com.group5.paul_esys.modules.rooms.model;

public class Room {

  private Long id;
  private String building;
  private String roomType;
  private String status;
  private String room;
  private Integer capacity;

  public Room() {
  }

  public Room(Long id, String room, Integer capacity) {
    this(id, null, null, null, room, capacity);
  }

  public Room(Long id, String building, String roomType, String status, String room, Integer capacity) {
    this.id = id;
    this.building = building;
    this.roomType = roomType;
    this.status = status;
    this.room = room;
    this.capacity = capacity;
  }

  public Long getId() {
    return id;
  }

  public Room setId(Long id) {
    this.id = id;
    return this;
  }

  public String getBuilding() {
    return building;
  }

  public Room setBuilding(String building) {
    this.building = building;
    return this;
  }

  public String getRoomType() {
    return roomType;
  }

  public Room setRoomType(String roomType) {
    this.roomType = roomType;
    return this;
  }

  public String getStatus() {
    return status;
  }

  public Room setStatus(String status) {
    this.status = status;
    return this;
  }

  public String getRoom() {
    return room;
  }

  public Room setRoom(String room) {
    this.room = room;
    return this;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public Room setCapacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }
}
