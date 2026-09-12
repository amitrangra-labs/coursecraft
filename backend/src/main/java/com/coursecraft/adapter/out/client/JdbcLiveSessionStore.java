package com.coursecraft.adapter.out.client;

import com.coursecraft.domain.object.LiveSession;
import com.coursecraft.domain.object.LiveStatus;
import com.coursecraft.port.LiveSessionStore;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@link LiveSessionStore} over Supabase Postgres via {@link JdbcClient}. */
public final class JdbcLiveSessionStore implements LiveSessionStore {

    private final JdbcClient jdbc;

    public JdbcLiveSessionStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public LiveSession insert(LiveSession s) {
        jdbc.sql("""
                        INSERT INTO live_session (id, course_id, title, youtube_video_id, status, starts_at, created_at)
                        VALUES (:id, :courseId, :title, :videoId, :status, :startsAt, now())
                        """)
                .param("id", s.id()).param("courseId", s.courseId())
                .param("title", s.title()).param("videoId", s.youtubeVideoId())
                .param("status", s.status().name())
                .param("startsAt", OffsetDateTime.ofInstant(s.startsAt(), java.time.ZoneOffset.UTC))
                .update();
        return find(s.id()).orElseThrow();
    }

    @Override
    public Optional<LiveSession> find(UUID id) {
        return jdbc.sql("""
                        SELECT id, course_id, title, youtube_video_id, status, starts_at, created_at
                        FROM live_session WHERE id = :id
                        """)
                .param("id", id).query(JdbcLiveSessionStore::map).optional();
    }

    @Override
    public List<LiveSession> listByCourse(UUID courseId) {
        return jdbc.sql("""
                        SELECT id, course_id, title, youtube_video_id, status, starts_at, created_at
                        FROM live_session WHERE course_id = :courseId ORDER BY starts_at DESC
                        """)
                .param("courseId", courseId).query(JdbcLiveSessionStore::map).list();
    }

    @Override
    public List<LiveSession> listLiveInPublishedCourses() {
        return jdbc.sql("""
                        SELECT s.id, s.course_id, s.title, s.youtube_video_id, s.status, s.starts_at, s.created_at
                        FROM live_session s
                        JOIN course c ON c.id = s.course_id
                        WHERE s.status = 'LIVE' AND c.status = 'PUBLISHED'
                        ORDER BY s.starts_at DESC
                        """)
                .query(JdbcLiveSessionStore::map).list();
    }

    @Override
    public void updateStatus(UUID id, LiveStatus status) {
        jdbc.sql("UPDATE live_session SET status = :status WHERE id = :id")
                .param("status", status.name()).param("id", id).update();
    }

    private static LiveSession map(ResultSet rs, int n) throws SQLException {
        return new LiveSession(
                rs.getObject("id", UUID.class),
                rs.getObject("course_id", UUID.class),
                rs.getString("title"),
                rs.getString("youtube_video_id"),
                LiveStatus.valueOf(rs.getString("status")),
                rs.getObject("starts_at", OffsetDateTime.class).toInstant(),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
