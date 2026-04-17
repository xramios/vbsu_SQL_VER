package com.group5.paul_esys.modules.registrar.model;

public record DuplicateSubjectDTO(
    Long enrollmentDetailId,
    String subjectCode,
    String section,
    String instructor,
    String source
) {
}
