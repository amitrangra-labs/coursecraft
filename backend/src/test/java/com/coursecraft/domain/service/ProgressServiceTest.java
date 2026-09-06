package com.coursecraft.domain.service;

import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.Section;
import com.coursecraft.domain.object.User;
import com.coursecraft.testsupport.InMemoryCourseStore;
import com.coursecraft.testsupport.InMemoryProgressStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressServiceTest {

    private final InMemoryCourseStore courseStore = new InMemoryCourseStore();
    private final CourseService courseService = new CourseService(courseStore);
    private final ProgressService progress = new ProgressService(new InMemoryProgressStore(), courseStore);

    private static User user(Role role) {
        return new User(UUID.randomUUID(), "sub-" + UUID.randomUUID(), "u@x.io", "u", role, Instant.now());
    }

    @Test
    void savesAndReadsPosition() {
        User learner = user(Role.LEARNER);
        UUID lectureId = UUID.randomUUID();
        progress.save(learner, lectureId, 42, false);
        assertEquals(42, progress.get(learner, lectureId).positionSec());
    }

    @Test
    void completedIsSticky() {
        User learner = user(Role.LEARNER);
        UUID lectureId = UUID.randomUUID();
        progress.save(learner, lectureId, 100, true);
        progress.save(learner, lectureId, 5, false); // rewound, but stays completed
        assertTrue(progress.get(learner, lectureId).completed());
    }

    @Test
    void courseProgressPercent() {
        User creator = user(Role.CREATOR);
        Course c = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        Section s = courseService.addSection(creator, c.id(), "Intro");
        Lecture l1 = courseService.addVideoLecture(creator, c.id(), s.id(), "L1", "youtube", "dQw4w9WgXcQ");
        courseService.addVideoLecture(creator, c.id(), s.id(), "L2", "youtube", "dQw4w9WgXcQ");

        User learner = user(Role.LEARNER);
        progress.save(learner, l1.id(), 60, true); // 1 of 2 complete

        var cp = progress.courseProgress(learner, c.id());
        assertEquals(50, cp.percent());
        assertEquals(1, cp.completed());
        assertEquals(2, cp.total());
    }
}
