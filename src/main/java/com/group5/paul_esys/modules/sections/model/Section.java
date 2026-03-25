package com.group5.paul_esys.modules.sections.model;

import java.sql.Timestamp;

public class Section {

  private Long id;
  private String sectionName;
  private String sectionCode;
  private Long subjectId;
  private Integer capacity;
  private Timestamp updatedAt;
  private Timestamp createdAt;

  public Section() {
  }

  public Section(Long id, String sectionName, String sectionCode, Long subjectId, Integer capacity, Timestamp updatedAt, Timestamp createdAt) {
    this.id = id;
    this.sectionName = sectionName;
    this.sectionCode = sectionCode;
    this.subjectId = subjectId;
    this.capacity = capacity;
    this.updatedAt = updatedAt;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Section setId(Long id) {
    this.id = id;
    return this;
  }

  public String getSectionName() {
    return sectionName;
  }

  public Section setSectionName(String sectionName) {
    this.sectionName = sectionName;
    return this;
  }

  public String getSectionCode() {
    return sectionCode;
  }

  public Section setSectionCode(String sectionCode) {
    this.sectionCode = sectionCode;
    return this;
  }

  public Long getSubjectId() {
    return subjectId;
  }

  public Section setSubjectId(Long subjectId) {
    this.subjectId = subjectId;
    return this;
  }

  public Integer getCapacity() {
    return capacity;
  }

  public Section setCapacity(Integer capacity) {
    this.capacity = capacity;
    return this;
  }

  public Timestamp getUpdatedAt() {
    return updatedAt;
  }

  public Section setUpdatedAt(Timestamp updatedAt) {
    this.updatedAt = updatedAt;
    return this;
  }

  public Timestamp getCreatedAt() {
    return createdAt;
  }

  public Section setCreatedAt(Timestamp createdAt) {
    this.createdAt = createdAt;
    return this;
  }
}
