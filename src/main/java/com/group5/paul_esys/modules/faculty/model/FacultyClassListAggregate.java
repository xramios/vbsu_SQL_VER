package com.group5.paul_esys.modules.faculty.model;

import java.util.LinkedHashSet;
import java.util.Set;

public final class FacultyClassListAggregate {

  private final Long offeringId;
  private final String code;
  private final String subject;
  private final String section;
  private final int students;
  private final Set<String> scheduleParts = new LinkedHashSet<>();
  private final Set<String> roomParts = new LinkedHashSet<>();

  public FacultyClassListAggregate(
      Long offeringId,
      String code,
      String subject,
      String section,
      int students
  ) {
    this.offeringId = offeringId;
    this.code = code;
    this.subject = subject;
    this.section = section;
    this.students = students;
  }

  public Set<String> scheduleParts() {
    return scheduleParts;
  }

  public Set<String> roomParts() {
    return roomParts;
  }

  public FacultyClassListRow toRow() {
    return new FacultyClassListRow(
        offeringId,
        code,
        subject,
        section,
        scheduleParts.isEmpty() ? "TBA" : String.join(" | ", scheduleParts),
        roomParts.isEmpty() ? "TBA" : String.join(", ", roomParts),
        students
    );
  }
}