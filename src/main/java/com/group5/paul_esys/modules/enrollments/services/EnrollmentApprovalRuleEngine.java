package com.group5.paul_esys.modules.enrollments.services;

import com.group5.paul_esys.modules.enrollments.model.EnrollmentDetail;
import com.group5.paul_esys.modules.offerings.services.OfferingService;
import com.group5.paul_esys.modules.schedules.model.Schedule;
import com.group5.paul_esys.modules.schedules.services.ScheduleConflictAnalyzer;
import com.group5.paul_esys.modules.schedules.services.ScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EnrollmentApprovalRuleEngine {
    private static final Logger logger = LoggerFactory.getLogger(EnrollmentApprovalRuleEngine.class);

    /**
     * Checks if an enrollment is eligible for automatic approval.
     * 
     * @param studentId The ID of the student
     * @param enrollmentId The ID of the enrollment
     * @param semester The semester being enrolled in
     * @param yearLevel The year level of the student
     * @param details The list of enrollment details (subjects/offerings)
     * @return true if all rules pass, false otherwise
     */
    public static boolean canAutoApprove(String studentId, Long enrollmentId, String semester, Long yearLevel, List<EnrollmentDetail> details) {
        // 1. Check Pre-requisites (Already partially handled by StudentEnrollmentEligibilityService)
        if (!checkPrerequisites(studentId, semester, yearLevel, details)) {
            logger.info("Auto-approval failed for student {}: Pre-requisites not met", studentId);
            return false;
        }

        // 2. Check Schedule Conflicts
        if (!checkScheduleConflicts(details)) {
            logger.info("Auto-approval failed for student {}: Schedule conflicts detected", studentId);
            return false;
        }

        // 3. Check Quotas
        if (!checkQuotas(details)) {
            logger.info("Auto-approval failed for student {}: Offering quotas exceeded", studentId);
            return false;
        }

        return true;
    }

    private static boolean checkPrerequisites(String studentId, String semester, Long yearLevel, List<EnrollmentDetail> details) {
        Set<Long> eligibleSemesterSubjectIds = StudentEnrollmentEligibilityService.getInstance()
                .getEligibleSemesterSubjectIds(studentId, semester, yearLevel);

        for (EnrollmentDetail detail : details) {
            final Long currentOfferingId = detail.getOfferingId();
            Long semesterSubjectId = OfferingService.getInstance()
                    .getOfferingById(currentOfferingId)
                    .map(o -> o.getSemesterSubjectId())
                    .orElse(null);

            if (semesterSubjectId == null || !eligibleSemesterSubjectIds.contains(semesterSubjectId)) {
                return false;
            }
        }
        return true;
    }

    private static boolean checkScheduleConflicts(List<EnrollmentDetail> details) {
        Map<Long, List<Schedule>> schedulesPerOffering = new HashMap<>();
        for (EnrollmentDetail detail : details) {
            List<Schedule> schedules = ScheduleService.getInstance().getSchedulesByOffering(detail.getOfferingId());
            schedulesPerOffering.put(detail.getOfferingId(), schedules);
        }

        return ScheduleConflictAnalyzer.getInstance().analyzeConflicts(schedulesPerOffering).isEmpty();
    }

    private static boolean checkQuotas(List<EnrollmentDetail> details) {
        for (EnrollmentDetail detail : details) {
            int currentEnrollmentCount = (int) EnrollmentDetailService.getInstance().countSelectedByOffering(detail.getOfferingId());
            int capacity = OfferingService.getInstance()
                    .getOfferingById(detail.getOfferingId())
                    .map(o -> o.getCapacity())
                    .orElse(0);

            if (currentEnrollmentCount >= capacity) {
                return false;
            }
        }
        return true;
    }
}
