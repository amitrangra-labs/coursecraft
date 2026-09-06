package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.NotFoundException;
import com.coursecraft.domain.object.Assessment;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseAnalytics;
import com.coursecraft.domain.object.CourseAnalytics.AssessmentStat;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.User;
import com.coursecraft.port.AssessmentStore;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.EnrollmentStore;

import java.util.List;
import java.util.UUID;

/** Creator analytics (journey CJ-6). Read-only aggregates over a creator's own course. */
public final class AnalyticsService {

    private final CourseStore courses;
    private final EnrollmentStore enrollments;
    private final AssessmentStore assessments;

    public AnalyticsService(CourseStore courses, EnrollmentStore enrollments, AssessmentStore assessments) {
        this.courses = courses;
        this.enrollments = enrollments;
        this.assessments = assessments;
    }

    public CourseAnalytics courseAnalytics(User creator, UUID courseId) {
        if (creator.role() != Role.CREATOR) {
            throw new ForbiddenException("Only creators can view analytics");
        }
        Course course = courses.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (!course.creatorId().equals(creator.id())) {
            throw new ForbiddenException("You do not own this course");
        }
        long enrolled = enrollments.countByCourse(courseId);
        List<AssessmentStat> stats = assessments.listByCourse(courseId).stream()
                .map(this::stat)
                .toList();
        return new CourseAnalytics(enrolled, stats);
    }

    private AssessmentStat stat(Assessment a) {
        long[] s = assessments.attemptStats(a.id());
        return new AssessmentStat(a.title(), s[0], (int) s[1]);
    }
}
