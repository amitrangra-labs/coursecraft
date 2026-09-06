package com.coursecraft.adapter.out.client;

import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.port.EnrollmentStore;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** {@link EnrollmentStore} over Supabase Postgres via {@link JdbcClient}. */
public final class JdbcEnrollmentStore implements EnrollmentStore {

    private final JdbcClient jdbc;

    public JdbcEnrollmentStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void enroll(UUID learnerId, UUID courseId) {
        jdbc.sql("""
                        INSERT INTO enrollment (id, learner_id, course_id, created_at)
                        VALUES (gen_random_uuid(), :learnerId, :courseId, now())
                        ON CONFLICT (learner_id, course_id) DO NOTHING
                        """)
                .param("learnerId", learnerId)
                .param("courseId", courseId)
                .update();
    }

    @Override
    public void unenroll(UUID learnerId, UUID courseId) {
        jdbc.sql("DELETE FROM enrollment WHERE learner_id = :learnerId AND course_id = :courseId")
                .param("learnerId", learnerId)
                .param("courseId", courseId)
                .update();
    }

    @Override
    public boolean isEnrolled(UUID learnerId, UUID courseId) {
        return jdbc.sql("""
                        SELECT 1 FROM enrollment WHERE learner_id = :learnerId AND course_id = :courseId
                        """)
                .param("learnerId", learnerId)
                .param("courseId", courseId)
                .query(Integer.class)
                .optional()
                .isPresent();
    }

    @Override
    public List<Course> listEnrolledCourses(UUID learnerId) {
        return jdbc.sql("""
                        SELECT c.id, c.creator_id, c.title, c.subject, c.level, c.status, c.created_at
                        FROM enrollment e
                        JOIN course c ON c.id = e.course_id
                        WHERE e.learner_id = :learnerId AND c.status = 'PUBLISHED'
                        ORDER BY e.created_at DESC
                        """)
                .param("learnerId", learnerId)
                .query(JdbcEnrollmentStore::mapCourse)
                .list();
    }

    private static Course mapCourse(ResultSet rs, int rowNum) throws SQLException {
        return new Course(
                rs.getObject("id", UUID.class),
                rs.getObject("creator_id", UUID.class),
                rs.getString("title"),
                rs.getString("subject"),
                rs.getString("level"),
                CourseStatus.valueOf(rs.getString("status")),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
