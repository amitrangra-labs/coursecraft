package com.coursecraft.adapter.out.client;

import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.LectureType;
import com.coursecraft.domain.object.Section;
import com.coursecraft.port.CourseStore;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@link CourseStore} over Supabase Postgres via {@link JdbcClient}. All SQL is visible here. */
public final class JdbcCourseStore implements CourseStore {

    private final JdbcClient jdbc;

    public JdbcCourseStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Course insertCourse(Course c) {
        jdbc.sql("""
                        INSERT INTO course (id, creator_id, title, subject, level, status, created_at)
                        VALUES (:id, :creatorId, :title, :subject, :level, :status, now())
                        """)
                .param("id", c.id())
                .param("creatorId", c.creatorId())
                .param("title", c.title())
                .param("subject", c.subject())
                .param("level", c.level())
                .param("status", c.status().name())
                .update();
        return findCourse(c.id()).orElseThrow();
    }

    @Override
    public Optional<Course> findCourse(UUID id) {
        return jdbc.sql("""
                        SELECT id, creator_id, title, subject, level, status, created_at
                        FROM course WHERE id = :id
                        """)
                .param("id", id)
                .query(JdbcCourseStore::mapCourse)
                .optional();
    }

    @Override
    public List<Course> listPublished() {
        return jdbc.sql("""
                        SELECT id, creator_id, title, subject, level, status, created_at
                        FROM course WHERE status = 'PUBLISHED' ORDER BY created_at DESC
                        """)
                .query(JdbcCourseStore::mapCourse)
                .list();
    }

    @Override
    public List<Course> listByCreator(UUID creatorId) {
        return jdbc.sql("""
                        SELECT id, creator_id, title, subject, level, status, created_at
                        FROM course WHERE creator_id = :creatorId ORDER BY created_at DESC
                        """)
                .param("creatorId", creatorId)
                .query(JdbcCourseStore::mapCourse)
                .list();
    }

    @Override
    public void updateStatus(UUID courseId, CourseStatus status) {
        jdbc.sql("UPDATE course SET status = :status WHERE id = :id")
                .param("status", status.name())
                .param("id", courseId)
                .update();
    }

    @Override
    public Section insertSection(Section s) {
        jdbc.sql("""
                        INSERT INTO course_section (id, course_id, title, position, created_at)
                        VALUES (:id, :courseId, :title, :position, now())
                        """)
                .param("id", s.id())
                .param("courseId", s.courseId())
                .param("title", s.title())
                .param("position", s.position())
                .update();
        return jdbc.sql("""
                        SELECT id, course_id, title, position, created_at
                        FROM course_section WHERE id = :id
                        """)
                .param("id", s.id())
                .query(JdbcCourseStore::mapSection)
                .single();
    }

    @Override
    public Optional<Section> findSection(UUID id) {
        return jdbc.sql("""
                        SELECT id, course_id, title, position, created_at
                        FROM course_section WHERE id = :id
                        """)
                .param("id", id)
                .query(JdbcCourseStore::mapSection)
                .optional();
    }

    @Override
    public List<Section> listSections(UUID courseId) {
        return jdbc.sql("""
                        SELECT id, course_id, title, position, created_at
                        FROM course_section WHERE course_id = :courseId ORDER BY position, created_at
                        """)
                .param("courseId", courseId)
                .query(JdbcCourseStore::mapSection)
                .list();
    }

    @Override
    public Lecture insertLecture(Lecture l) {
        jdbc.sql("""
                        INSERT INTO lecture
                            (id, section_id, title, type, video_provider, video_id, position, created_at)
                        VALUES
                            (:id, :sectionId, :title, :type, :provider, :videoId, :position, now())
                        """)
                .param("id", l.id())
                .param("sectionId", l.sectionId())
                .param("title", l.title())
                .param("type", l.type().name())
                .param("provider", l.videoProvider())
                .param("videoId", l.videoId())
                .param("position", l.position())
                .update();
        return jdbc.sql("""
                        SELECT id, section_id, title, type, video_provider, video_id, position, created_at
                        FROM lecture WHERE id = :id
                        """)
                .param("id", l.id())
                .query(JdbcCourseStore::mapLecture)
                .single();
    }

    @Override
    public List<Lecture> listLecturesBySection(UUID sectionId) {
        return jdbc.sql("""
                        SELECT id, section_id, title, type, video_provider, video_id, position, created_at
                        FROM lecture WHERE section_id = :sectionId ORDER BY position, created_at
                        """)
                .param("sectionId", sectionId)
                .query(JdbcCourseStore::mapLecture)
                .list();
    }

    @Override
    public List<Lecture> listLecturesByCourse(UUID courseId) {
        return jdbc.sql("""
                        SELECT l.id, l.section_id, l.title, l.type, l.video_provider, l.video_id,
                               l.position, l.created_at
                        FROM lecture l
                        JOIN course_section s ON s.id = l.section_id
                        WHERE s.course_id = :courseId
                        ORDER BY s.position, l.position
                        """)
                .param("courseId", courseId)
                .query(JdbcCourseStore::mapLecture)
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

    private static Section mapSection(ResultSet rs, int rowNum) throws SQLException {
        return new Section(
                rs.getObject("id", UUID.class),
                rs.getObject("course_id", UUID.class),
                rs.getString("title"),
                rs.getInt("position"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }

    private static Lecture mapLecture(ResultSet rs, int rowNum) throws SQLException {
        return new Lecture(
                rs.getObject("id", UUID.class),
                rs.getObject("section_id", UUID.class),
                rs.getString("title"),
                LectureType.valueOf(rs.getString("type")),
                rs.getString("video_provider"),
                rs.getString("video_id"),
                rs.getInt("position"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }
}
