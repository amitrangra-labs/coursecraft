package com.coursecraft.testsupport;

import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.Section;
import com.coursecraft.port.CourseStore;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Simple in-memory {@link CourseStore} for tests — no database. */
public final class InMemoryCourseStore implements CourseStore {

    private final Map<UUID, Course> courses = new LinkedHashMap<>();
    private final Map<UUID, Section> sections = new LinkedHashMap<>();
    private final Map<UUID, Lecture> lectures = new LinkedHashMap<>();

    @Override
    public Course insertCourse(Course course) {
        Course stored = withCreatedAt(course);
        courses.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<Course> findCourse(UUID id) {
        return Optional.ofNullable(courses.get(id));
    }

    @Override
    public List<Course> listPublished() {
        return courses.values().stream()
                .filter(c -> c.status() == CourseStatus.PUBLISHED)
                .toList();
    }

    @Override
    public List<Course> listByCreator(UUID creatorId) {
        return courses.values().stream().filter(c -> c.creatorId().equals(creatorId)).toList();
    }

    @Override
    public void updateStatus(UUID courseId, CourseStatus status) {
        Course c = courses.get(courseId);
        courses.put(courseId, new Course(c.id(), c.creatorId(), c.title(), c.subject(), c.level(),
                status, c.createdAt()));
    }

    @Override
    public Section insertSection(Section section) {
        Section stored = new Section(section.id(), section.courseId(), section.title(),
                section.position(), Instant.now());
        sections.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<Section> findSection(UUID id) {
        return Optional.ofNullable(sections.get(id));
    }

    @Override
    public List<Section> listSections(UUID courseId) {
        return sections.values().stream()
                .filter(s -> s.courseId().equals(courseId))
                .sorted(Comparator.comparingInt(Section::position))
                .toList();
    }

    @Override
    public void updateCourse(UUID id, String title, String subject, String level) {
        Course c = courses.get(id);
        courses.put(id, new Course(c.id(), c.creatorId(), title, subject, level, c.status(), c.createdAt()));
    }

    @Override
    public void deleteCourse(UUID id) {
        courses.remove(id);
        List<UUID> secIds = sections.values().stream()
                .filter(s -> s.courseId().equals(id)).map(Section::id).toList();
        secIds.forEach(this::deleteSection);
    }

    @Override
    public void updateSection(UUID id, String title) {
        Section s = sections.get(id);
        sections.put(id, new Section(s.id(), s.courseId(), title, s.position(), s.createdAt()));
    }

    @Override
    public void deleteSection(UUID id) {
        sections.remove(id);
        lectures.values().removeIf(l -> l.sectionId().equals(id));
    }

    @Override
    public void updateSectionPositions(List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            Section s = sections.get(orderedIds.get(i));
            sections.put(s.id(), new Section(s.id(), s.courseId(), s.title(), i, s.createdAt()));
        }
    }

    @Override
    public Lecture insertLecture(Lecture lecture) {
        Lecture stored = new Lecture(lecture.id(), lecture.sectionId(), lecture.title(),
                lecture.type(), lecture.videoProvider(), lecture.videoId(), lecture.position(),
                Instant.now());
        lectures.put(stored.id(), stored);
        return stored;
    }

    @Override
    public Optional<Lecture> findLecture(UUID id) {
        return Optional.ofNullable(lectures.get(id));
    }

    @Override
    public void updateLecture(UUID id, String title, String provider, String videoId) {
        Lecture l = lectures.get(id);
        lectures.put(id, new Lecture(l.id(), l.sectionId(), title, l.type(), provider, videoId,
                l.position(), l.createdAt()));
    }

    @Override
    public void deleteLecture(UUID id) {
        lectures.remove(id);
    }

    @Override
    public void updateLecturePositions(List<UUID> orderedIds) {
        for (int i = 0; i < orderedIds.size(); i++) {
            Lecture l = lectures.get(orderedIds.get(i));
            lectures.put(l.id(), new Lecture(l.id(), l.sectionId(), l.title(), l.type(),
                    l.videoProvider(), l.videoId(), i, l.createdAt()));
        }
    }

    @Override
    public List<Lecture> listLecturesBySection(UUID sectionId) {
        return lectures.values().stream()
                .filter(l -> l.sectionId().equals(sectionId))
                .sorted(Comparator.comparingInt(Lecture::position))
                .toList();
    }

    @Override
    public List<Lecture> listLecturesByCourse(UUID courseId) {
        List<UUID> sectionIds = listSections(courseId).stream().map(Section::id).toList();
        List<Lecture> out = new ArrayList<>();
        for (UUID sid : sectionIds) {
            out.addAll(listLecturesBySection(sid));
        }
        return out;
    }

    private static Course withCreatedAt(Course c) {
        return new Course(c.id(), c.creatorId(), c.title(), c.subject(), c.level(), c.status(),
                c.createdAt() == null ? Instant.now() : c.createdAt());
    }
}
