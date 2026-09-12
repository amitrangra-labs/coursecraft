package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.LiveSession;
import com.coursecraft.domain.object.LiveStatus;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.User;
import com.coursecraft.testsupport.InMemoryCourseStore;
import com.coursecraft.testsupport.InMemoryLiveSessionStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LiveSessionServiceTest {

    private final InMemoryCourseStore courseStore = new InMemoryCourseStore();
    private final CourseService courseService = new CourseService(courseStore);
    private final LiveSessionService live = new LiveSessionService(new InMemoryLiveSessionStore(), courseStore);

    private static User user(Role role) {
        return new User(UUID.randomUUID(), "sub-" + UUID.randomUUID(), "u@x.io", "u", role, Instant.now());
    }

    @Test
    void learnerCannotSchedule() {
        User creator = user(Role.CREATOR);
        Course c = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        assertThrows(ForbiddenException.class,
                () -> live.schedule(user(Role.LEARNER), c.id(), "Lecture", "dQw4w9WgXcQ", Instant.now()));
    }

    @Test
    void scheduleNormalizesUrlAndLifecycle() {
        User creator = user(Role.CREATOR);
        Course c = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        LiveSession s = live.schedule(creator, c.id(), "Lecture",
                "https://www.youtube.com/live/dQw4w9WgXcQ", Instant.now());
        assertEquals("dQw4w9WgXcQ", s.youtubeVideoId());
        assertEquals(LiveStatus.SCHEDULED, s.status());

        assertEquals(LiveStatus.LIVE, live.goLive(creator, s.id()).status());
        assertEquals(LiveStatus.ENDED, live.end(creator, s.id()).status());
    }

    @Test
    void keepsSelfHostedStreamUrlAsIs() {
        User creator = user(Role.CREATOR);
        Course c = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        String hls = "https://live.example.org/hls/lecture.m3u8";
        LiveSession s = live.schedule(creator, c.id(), "In-house live", hls, Instant.now());
        assertEquals(hls, s.youtubeVideoId());
    }

    @Test
    void rejectsBadVideoRef() {
        User creator = user(Role.CREATOR);
        Course c = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        assertThrows(ValidationException.class,
                () -> live.schedule(creator, c.id(), "Lecture", "short", Instant.now()));
    }
}
