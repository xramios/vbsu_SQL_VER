package com.group5.paul_esys.modules.faculty.model;

public record FacultyClassListRow(
    Long offeringId,
    String code,
    String subject,
    String section,
    String schedule,
    String room,
    int students
) {
}