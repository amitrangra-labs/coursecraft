package com.coursecraft.testsupport;

import com.coursecraft.domain.object.LiveSession;
import com.coursecraft.domain.object.LiveStatus;
import com.coursecraft.port.LiveSessionStore;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory {@link LiveSessionStore} for tests. */
public final class InMemoryLiveSessionStore implements LiveSessionStore {

    private final Map<UUID, LiveSession> sessions = new LinkedHashMap<>();

    @Override
    public LiveSession insert(LiveSession s) {
        LiveSession stored = new LiveSession(s.id(), s.courseId(), s.title(), s.youtubeVideoId(),
                s.status(), s.startsAt(), Instant.now());
        sessions.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<LiveSession> find(UUID id) {
        return Optional.ofNullable(sessions.get(id));
    }

    @Override
    public List<LiveSession> listByCourse(UUID courseId) {
        return sessions.values().stream().filter(s -> s.courseId().equals(courseId)).toList();
    }

    @Override
    public List<LiveSession> listLiveInPublishedCourses() {
        return sessions.values().stream().filter(s -> s.status() == LiveStatus.LIVE).toList();
    }

    @Override
    public void updateStatus(UUID id, LiveStatus status) {
        LiveSession s = sessions.get(id);
        sessions.put(id, new LiveSession(s.id(), s.courseId(), s.title(), s.youtubeVideoId(),
                status, s.startsAt(), s.createdAt()));
    }
}
