package com.coursecraft.testsupport;

import com.coursecraft.domain.object.Course;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.EnrollmentStore;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** In-memory {@link EnrollmentStore} for tests; resolves courses via a {@link CourseStore}. */
public final class InMemoryEnrollmentStore implements EnrollmentStore {

    private record Key(UUID learner, UUID course) {
    }

    private final Set<Key> enrollments = new LinkedHashSet<>();
    private final CourseStore courses;

    public InMemoryEnrollmentStore(CourseStore courses) {
        this.courses = courses;
    }

    @Override
    public void enroll(UUID learnerId, UUID courseId) {
        enrollments.add(new Key(learnerId, courseId));
    }

    @Override
    public void unenroll(UUID learnerId, UUID courseId) {
        enrollments.remove(new Key(learnerId, courseId));
    }

    @Override
    public boolean isEnrolled(UUID learnerId, UUID courseId) {
        return enrollments.contains(new Key(learnerId, courseId));
    }

    @Override
    public List<Course> listEnrolledCourses(UUID learnerId) {
        List<Course> out = new ArrayList<>();
        for (Key k : enrollments) {
            if (k.learner().equals(learnerId)) {
                courses.findCourse(k.course()).ifPresent(out::add);
            }
        }
        return out;
    }
}
