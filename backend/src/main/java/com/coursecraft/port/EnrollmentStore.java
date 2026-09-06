package com.coursecraft.port;

import com.coursecraft.domain.object.Course;

import java.util.List;
import java.util.UUID;

/** Outbound port for learner enrollments. */
public interface EnrollmentStore {

    /** Idempotent — enrolling twice is a no-op. */
    void enroll(UUID learnerId, UUID courseId);

    void unenroll(UUID learnerId, UUID courseId);

    boolean isEnrolled(UUID learnerId, UUID courseId);

    /** Published courses the learner is enrolled in, most recent first. */
    List<Course> listEnrolledCourses(UUID learnerId);
}
