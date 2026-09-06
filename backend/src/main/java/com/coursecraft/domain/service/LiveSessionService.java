package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.NotFoundException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.LiveSession;
import com.coursecraft.domain.object.LiveStatus;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.User;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.LiveSessionStore;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Live lecture scheduling + lifecycle (journeys CJ-7, LJ-7). */
public final class LiveSessionService {

    private static final Pattern YOUTUBE = Pattern.compile(
            "(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|live/|shorts/))([A-Za-z0-9_-]{11})");

    private final LiveSessionStore store;
    private final CourseStore courses;

    public LiveSessionService(LiveSessionStore store, CourseStore courses) {
        this.store = store;
        this.courses = courses;
    }

    public LiveSession schedule(User creator, UUID courseId, String title, String rawVideoRef, Instant startsAt) {
        ownedCourse(creator, courseId);
        if (title == null || title.isBlank()) {
            throw new ValidationException("title is required");
        }
        String videoId = normalizeYoutube(rawVideoRef);
        Instant start = startsAt == null ? Instant.now() : startsAt;
        return store.insert(new LiveSession(UUID.randomUUID(), courseId, title.trim(), videoId,
                LiveStatus.SCHEDULED, start, null));
    }

    public LiveSession goLive(User creator, UUID sessionId) {
        return transition(creator, sessionId, LiveStatus.LIVE);
    }

    public LiveSession end(User creator, UUID sessionId) {
        return transition(creator, sessionId, LiveStatus.ENDED);
    }

    public LiveSession cancel(User creator, UUID sessionId) {
        return transition(creator, sessionId, LiveStatus.CANCELED);
    }

    public List<LiveSession> listForCourse(User viewer, UUID courseId) {
        requireVisibleCourse(viewer, courseId);
        return store.listByCourse(courseId);
    }

    // --- helpers ---

    private LiveSession transition(User creator, UUID sessionId, LiveStatus status) {
        LiveSession session = store.find(sessionId)
                .orElseThrow(() -> new NotFoundException("Live session not found"));
        ownedCourse(creator, session.courseId());
        store.updateStatus(sessionId, status);
        return store.find(sessionId).orElseThrow();
    }

    private Course ownedCourse(User creator, UUID courseId) {
        if (creator.role() != Role.CREATOR) {
            throw new ForbiddenException("Only creators can host live sessions");
        }
        Course course = courses.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (!course.creatorId().equals(creator.id())) {
            throw new ForbiddenException("You do not own this course");
        }
        return course;
    }

    private void requireVisibleCourse(User viewer, UUID courseId) {
        Course course = courses.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (course.status() != CourseStatus.PUBLISHED && !course.creatorId().equals(viewer.id())) {
            throw new ForbiddenException("Course is not published");
        }
    }

    private static String normalizeYoutube(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("YouTube video id or URL is required");
        }
        String trimmed = raw.trim();
        Matcher m = YOUTUBE.matcher(trimmed);
        if (m.find()) {
            return m.group(1);
        }
        if (trimmed.matches("[A-Za-z0-9_-]{11}")) {
            return trimmed;
        }
        throw new ValidationException("Not a valid YouTube video id or URL");
    }
}
