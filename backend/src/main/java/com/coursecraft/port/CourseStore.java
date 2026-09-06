package com.coursecraft.port;

import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.Section;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for course/section/lecture persistence (Supabase Postgres via JdbcClient). */
public interface CourseStore {

    Course insertCourse(Course course);

    Optional<Course> findCourse(UUID id);

    List<Course> listPublished();

    List<Course> listByCreator(UUID creatorId);

    void updateStatus(UUID courseId, CourseStatus status);

    Section insertSection(Section section);

    Optional<Section> findSection(UUID id);

    List<Section> listSections(UUID courseId);

    void updateCourse(UUID id, String title, String subject, String level);

    void deleteCourse(UUID id);

    void updateSection(UUID id, String title);

    void deleteSection(UUID id);

    /** Set each section's position to its index in the given order. */
    void updateSectionPositions(List<UUID> orderedIds);

    Lecture insertLecture(Lecture lecture);

    Optional<Lecture> findLecture(UUID id);

    void updateLecture(UUID id, String title, String provider, String videoId);

    void deleteLecture(UUID id);

    /** Set each lecture's position to its index in the given order. */
    void updateLecturePositions(List<UUID> orderedIds);

    List<Lecture> listLecturesBySection(UUID sectionId);

    List<Lecture> listLecturesByCourse(UUID courseId);
}
