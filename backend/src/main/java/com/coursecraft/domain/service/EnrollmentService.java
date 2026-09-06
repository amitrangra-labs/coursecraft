package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.NotFoundException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.User;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.EnrollmentStore;

import java.util.List;
import java.util.UUID;

/** Enrollment logic (journey LJ-1). Learners may enroll only in published courses. */
public final class EnrollmentService {

    private final EnrollmentStore enrollments;
    private final CourseStore courses;

    public EnrollmentService(EnrollmentStore enrollments, CourseStore courses) {
        this.enrollments = enrollments;
        this.courses = courses;
    }

    public void enroll(User learner, UUID courseId) {
        Course course = courses.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (course.status() != CourseStatus.PUBLISHED) {
            throw new ValidationException("Course is not published");
        }
        enrollments.enroll(learner.id(), courseId);
    }

    public void unenroll(User learner, UUID courseId) {
        enrollments.unenroll(learner.id(), courseId);
    }

    public boolean isEnrolled(User learner, UUID courseId) {
        return enrollments.isEnrolled(learner.id(), courseId);
    }

    public List<Course> myLearning(User learner) {
        return enrollments.listEnrolledCourses(learner.id());
    }
}
