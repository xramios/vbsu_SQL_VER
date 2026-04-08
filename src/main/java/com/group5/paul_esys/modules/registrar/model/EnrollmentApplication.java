package com.group5.paul_esys.modules.registrar.model;

import com.group5.paul_esys.modules.enums.EnrollmentStatus;

public class EnrollmentApplication {

  private Long enrollmentId;
  private String studentId;
  private String firstName;
  private String lastName;
  private Integer yearLevel;
  private EnrollmentStatus status;

  public EnrollmentApplication() {
  }

  public EnrollmentApplication(Long enrollmentId, String studentId, String firstName, String lastName, Integer yearLevel, EnrollmentStatus status) {
    this.enrollmentId = enrollmentId;
    this.studentId = studentId;
    this.firstName = firstName;
    this.lastName = lastName;
    this.yearLevel = yearLevel;
    this.status = status;
  }

  public Long getEnrollmentId() {
    return enrollmentId;
  }

  public EnrollmentApplication setEnrollmentId(Long enrollmentId) {
    this.enrollmentId = enrollmentId;
    return this;
  }

  public String getStudentId() {
    return studentId;
  }

  public EnrollmentApplication setStudentId(String studentId) {
    this.studentId = studentId;
    return this;
  }

  public String getFirstName() {
    return firstName;
  }

  public EnrollmentApplication setFirstName(String firstName) {
    this.firstName = firstName;
    return this;
  }

  public String getLastName() {
    return lastName;
  }

  public EnrollmentApplication setLastName(String lastName) {
    this.lastName = lastName;
    return this;
  }

  public Integer getYearLevel() {
    return yearLevel;
  }

  public EnrollmentApplication setYearLevel(Integer yearLevel) {
    this.yearLevel = yearLevel;
    return this;
  }

  public EnrollmentStatus getStatus() {
    return status;
  }

  public EnrollmentApplication setStatus(EnrollmentStatus status) {
    this.status = status;
    return this;
  }
}
