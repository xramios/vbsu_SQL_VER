package com.group5.paul_esys.modules.prerequisites.model;

import java.sql.Timestamp;

public class Prerequisite {

  private Long id;
  private Long preSubjectId;
  private Long subjectId;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Prerequisite() {
  }

  public Prerequisite(Long id, Long preSubjectId, Long subjectId, Timestamp updatedAt, Timestamp createdAt) {
    this.id = id;
    this.preSubjectId = preSubjectId;
    this.subjectId = subjectId;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Prerequisite setId(Long id) {
    this.id = id;
    return this;
  }

  public Long getPreSubjectId() {
    return preSubjectId;
  }

  public Prerequisite setPreSubjectId(Long preSubjectId) {
    this.preSubjectId = preSubjectId;
    return this;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public Prerequisite setSubjectId(Long subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Prerequisite setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Prerequisite setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
