package com.group5.paul_esys.modules.semester_subjects.model;

import java.sql.Timestamp;

public class SemesterSubject {

  private Long id;
  private Long semesterId;
  private Long subjectId;
  private Timestamp createdAt;
  private Timestamp updatedAt;

  public SemesterSubject() {
  }

  public SemesterSubject(Long id, Long semesterId, Long subjectId, Timestamp createdAt, Timestamp updatedAt) {
    this.id = id;
    this.semesterId = semesterId;
    this.subjectId = subjectId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public SemesterSubject setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getSemesterId() {
    return semesterId;
  }

  public SemesterSubject setSemesterId(Long semesterId) {
    this.semesterId = semesterId;
    return this;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public SemesterSubject setSubjectId(Long subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public SemesterSubject setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public SemesterSubject setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }
}