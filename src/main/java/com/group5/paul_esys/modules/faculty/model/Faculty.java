package com.group5.paul_esys.modules.faculty.model;

import java.sql.Timestamp;
import java.util.Date;

public class Faculty {

  private Long id;
  private Long userId;
  private String firstName;
  private String lastName;
  private String middleName;
  private String contactNumber;
  private Date birthdate;
  private Long departmentId;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Faculty() {
  }

  public Faculty(
      Long id,
      Long userId,
      String firstName,
      String lastName,
      String middleName,
      String contactNumber,
      Date birthdate,
      Long departmentId,
      Timestamp updatedAt,
      Timestamp createdAt
  ) {
    this.id = id;
    this.userId = userId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.middleName = middleName;
    this.contactNumber = contactNumber;
    this.birthdate = birthdate;
    this.departmentId = departmentId;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Faculty setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getUserId() {
    return userId;
  }

  public Faculty setUserId(Long userId) {
    this.userId = userId;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public Faculty setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public Faculty setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public String getMiddleName() {
    return middleName;
  }

  public Faculty setMiddleName(String middleName) {
    this.middleName = middleName;
    return this;
  }

  public String getContactNumber() {
    return contactNumber;
  }

  public Faculty setContactNumber(String contactNumber) {
    this.contactNumber = contactNumber;
    return this;
  }

  public Date getBirthdate() {
    return birthdate;
  }

  public Faculty setBirthdate(Date birthdate) {
    this.birthdate = birthdate;
    return this;
  }

  public Long getDepartmentId() {
    return departmentId;
  }

  public Faculty setDepartmentId(Long departmentId) {
    this.departmentId = departmentId;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Faculty setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Faculty setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
