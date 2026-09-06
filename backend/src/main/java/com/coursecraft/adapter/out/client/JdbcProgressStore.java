package com.coursecraft.adapter.out.client;

import com.coursecraft.domain.object.ContinueItem;
import com.coursecraft.domain.object.Progress;
import com.coursecraft.port.ProgressStore;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@link ProgressStore} over Supabase Postgres via {@link JdbcClient}. */
public final class JdbcProgressStore implements ProgressStore {

    private final JdbcClient jdbc;

    public JdbcProgressStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(UUID learnerId, UUID lectureId, int positionSec, boolean completed) {
        // completed is sticky: once true it stays true (OR with the incoming value).
        jdbc.sql("""
                        INSERT INTO progress (learner_id, lecture_id, position_sec, completed, updated_at)
                        VALUES (:learnerId, :lectureId, :positionSec, :completed, now())
                        ON CONFLICT (learner_id, lecture_id) DO UPDATE
                        SET position_sec = EXCLUDED.position_sec,
                            completed = progress.completed OR EXCLUDED.completed,
                            updated_at = now()
                        """)
                .param("learnerId", learnerId)
                .param("lectureId", lectureId)
                .param("positionSec", positionSec)
                .param("completed", completed)
                .update();
    }

    @Override
    public Optional<Progress> find(UUID learnerId, UUID lectureId) {
        return jdbc.sql("""
                        SELECT learner_id, lecture_id, position_sec, completed, updated_at
                        FROM progress WHERE learner_id = :learnerId AND lecture_id = :lectureId
                        """)
                .param("learnerId", learnerId)
                .param("lectureId", lectureId)
                .query(JdbcProgressStore::mapProgress)
                .optional();
    }

    @Override
    public List<Progress> listByCourse(UUID learnerId, UUID courseId) {
        return jdbc.sql("""
                        SELECT p.learner_id, p.lecture_id, p.position_sec, p.completed, p.updated_at
                        FROM progress p
                        JOIN lecture l ON l.id = p.lecture_id
                        JOIN course_section s ON s.id = l.section_id
                        WHERE p.learner_id = :learnerId AND s.course_id = :courseId
                        """)
                .param("learnerId", learnerId)
                .param("courseId", courseId)
                .query(JdbcProgressStore::mapProgress)
                .list();
    }

    @Override
    public Optional<ContinueItem> mostRecent(UUID learnerId) {
        return jdbc.sql("""
                        SELECT c.id AS course_id, c.title AS course_title,
                               l.id AS lecture_id, l.title AS lecture_title, l.video_id,
                               p.position_sec
                        FROM progress p
                        JOIN lecture l ON l.id = p.lecture_id
                        JOIN course_section s ON s.id = l.section_id
                        JOIN course c ON c.id = s.course_id
                        WHERE p.learner_id = :learnerId
                        ORDER BY p.updated_at DESC
                        LIMIT 1
                        """)
                .param("learnerId", learnerId)
                .query((rs, n) -> new ContinueItem(
                        rs.getObject("course_id", UUID.class),
                        rs.getString("course_title"),
                        rs.getObject("lecture_id", UUID.class),
                        rs.getString("lecture_title"),
                        rs.getString("video_id"),
                        rs.getInt("position_sec")))
                .optional();
    }

    private static Progress mapProgress(ResultSet rs, int n) throws SQLException {
        return new Progress(
                rs.getObject("learner_id", UUID.class),
                rs.getObject("lecture_id", UUID.class),
                rs.getInt("position_sec"),
                rs.getBoolean("completed"),
                rs.getObject("updated_at", OffsetDateTime.class).toInstant());
    }
}
