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

    Lecture insertLecture(Lecture lecture);

    List<Lecture> listLecturesBySection(UUID sectionId);

    List<Lecture> listLecturesByCourse(UUID courseId);
}
