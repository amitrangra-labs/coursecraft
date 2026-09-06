package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.Section;
import com.coursecraft.domain.object.User;
import com.coursecraft.testsupport.InMemoryCourseStore;
import com.coursecraft.testsupport.InMemoryEnrollmentStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnrollmentServiceTest {

    private final InMemoryCourseStore courseStore = new InMemoryCourseStore();
    private final CourseService courseService = new CourseService(courseStore);
    private final EnrollmentService enrollmentService =
            new EnrollmentService(new InMemoryEnrollmentStore(courseStore), courseStore);

    private static User user(Role role) {
        return new User(UUID.randomUUID(), "sub-" + UUID.randomUUID(), "u@x.io", "u", role, Instant.now());
    }

    private Course publishedCourse(User creator) {
        Course c = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        Section s = courseService.addSection(creator, c.id(), "Intro");
        courseService.addVideoLecture(creator, c.id(), s.id(), "Welcome", "youtube", "dQw4w9WgXcQ");
        return courseService.publish(creator, c.id());
    }

    @Test
    void cannotEnrollInDraftCourse() {
        User creator = user(Role.CREATOR);
        Course draft = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        assertThrows(ValidationException.class, () -> enrollmentService.enroll(user(Role.LEARNER), draft.id()));
    }

    @Test
    void enrollThenAppearsInMyLearning() {
        Course course = publishedCourse(user(Role.CREATOR));
        User learner = user(Role.LEARNER);

        enrollmentService.enroll(learner, course.id());

        assertTrue(enrollmentService.isEnrolled(learner, course.id()));
        assertEquals(1, enrollmentService.myLearning(learner).size());
    }

    @Test
    void unenrollRemovesFromMyLearning() {
        Course course = publishedCourse(user(Role.CREATOR));
        User learner = user(Role.LEARNER);
        enrollmentService.enroll(learner, course.id());

        enrollmentService.unenroll(learner, course.id());

        assertFalse(enrollmentService.isEnrolled(learner, course.id()));
        assertEquals(0, enrollmentService.myLearning(learner).size());
    }

    @Test
    void enrollIsIdempotent() {
        Course course = publishedCourse(user(Role.CREATOR));
        User learner = user(Role.LEARNER);
        enrollmentService.enroll(learner, course.id());
        enrollmentService.enroll(learner, course.id());
        assertEquals(1, enrollmentService.myLearning(learner).size());
    }
}
