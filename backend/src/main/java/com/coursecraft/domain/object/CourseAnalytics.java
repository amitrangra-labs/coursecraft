package com.coursecraft.domain.object;

import java.util.List;

/** Creator dashboard figures for a course (journey CJ-6). */
public record CourseAnalytics(long enrollments, List<AssessmentStat> assessments) {

    /** Per-assessment aggregate: number of attempts and average score as a percentage. */
    public record AssessmentStat(String title, long attempts, int averagePercent) {
    }
}
