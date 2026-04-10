package com.group5.paul_esys.modules.registrar.model;

import com.group5.paul_esys.modules.enums.DayOfWeek;
import java.time.LocalTime;

public record ScheduleUpsertRequest(
    Long scheduleId,
    Long offeringId,
    Long roomId,
    Long facultyId,
    DayOfWeek day,
    LocalTime startTime,
    LocalTime endTime
) {
}