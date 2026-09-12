package com.coursecraft.port;

import com.coursecraft.domain.object.LiveSession;
import com.coursecraft.domain.object.LiveStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for live sessions. */
public interface LiveSessionStore {

    LiveSession insert(LiveSession session);

    Optional<LiveSession> find(UUID id);

    List<LiveSession> listByCourse(UUID courseId);

    /** All currently-LIVE sessions in published courses (for a global "Live now" list). */
    List<LiveSession> listLiveInPublishedCourses();

    void updateStatus(UUID id, LiveStatus status);
}
