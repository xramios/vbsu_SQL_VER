package com.group5.paul_esys.modules.registrar.model;

public record ScheduleOfferingOption(
    Long offeringId,
    Long enrollmentPeriodId,
    String enrollmentPeriodLabel,
    String sectionCode,
    String subjectCode,
    String subjectName,
    String label
) {
}