package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.NotFoundException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseDetail;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.LectureType;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.Section;
import com.coursecraft.domain.object.User;
import com.coursecraft.port.CourseStore;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authoring + catalog logic (journeys CJ-2/CJ-5, LJ-1). Framework-free; enforces creator role and
 * course ownership server-side — the client is never trusted for these.
 */
public final class CourseService {

    private static final Pattern YOUTUBE = Pattern.compile(
            "(?:youtu\\.be/|youtube\\.com/(?:watch\\?v=|embed/|shorts/))([A-Za-z0-9_-]{11})");

    private final CourseStore store;

    public CourseService(CourseStore store) {
        this.store = store;
    }

    // --- Creator authoring ---

    public Course createCourse(User creator, String title, String subject, String level) {
        requireCreator(creator);
        String cleanTitle = requireText(title, "title");
        Course course = new Course(UUID.randomUUID(), creator.id(), cleanTitle,
                blankToNull(subject), blankToNull(level), CourseStatus.DRAFT, null);
        return store.insertCourse(course);
    }

    public Section addSection(User creator, UUID courseId, String title) {
        getOwnedCourse(creator, courseId);
        String cleanTitle = requireText(title, "title");
        int position = store.listSections(courseId).size();
        return store.insertSection(new Section(UUID.randomUUID(), courseId, cleanTitle, position, null));
    }

    public Lecture addVideoLecture(User creator, UUID courseId, UUID sectionId,
                                   String title, String provider, String rawVideoRef) {
        getOwnedCourse(creator, courseId);
        Section section = store.findSection(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        if (!section.courseId().equals(courseId)) {
            throw new ValidationException("Section does not belong to this course");
        }
        String cleanTitle = requireText(title, "title");
        String cleanProvider = blankToNull(provider) == null ? "youtube" : provider.trim().toLowerCase();
        String videoId = normalizeVideoId(cleanProvider, requireText(rawVideoRef, "videoId"));
        int position = store.listLecturesBySection(sectionId).size();
        return store.insertLecture(new Lecture(UUID.randomUUID(), sectionId, cleanTitle,
                LectureType.VIDEO, cleanProvider, videoId, position, null));
    }

    public Course publish(User creator, UUID courseId) {
        Course course = getOwnedCourse(creator, courseId);
        if (store.listSections(courseId).isEmpty()) {
            throw new ValidationException("Add at least one section before publishing");
        }
        if (store.listLecturesByCourse(courseId).isEmpty()) {
            throw new ValidationException("Add at least one lecture before publishing");
        }
        store.updateStatus(course.id(), CourseStatus.PUBLISHED);
        return store.findCourse(courseId).orElseThrow();
    }

    public List<Course> listMyCourses(User creator) {
        return store.listByCreator(creator.id());
    }

    // --- Creator editing / reordering ---

    public Course renameCourse(User creator, UUID courseId, String title, String subject, String level) {
        getOwnedCourse(creator, courseId);
        store.updateCourse(courseId, requireText(title, "title"), blankToNull(subject), blankToNull(level));
        return store.findCourse(courseId).orElseThrow();
    }

    public void deleteCourse(User creator, UUID courseId) {
        getOwnedCourse(creator, courseId);
        store.deleteCourse(courseId);
    }

    public Section renameSection(User creator, UUID courseId, UUID sectionId, String title) {
        ownedSection(creator, courseId, sectionId);
        store.updateSection(sectionId, requireText(title, "title"));
        return store.findSection(sectionId).orElseThrow();
    }

    public void deleteSection(User creator, UUID courseId, UUID sectionId) {
        ownedSection(creator, courseId, sectionId);
        store.deleteSection(sectionId);
    }

    public void reorderSections(User creator, UUID courseId, List<UUID> orderedIds) {
        getOwnedCourse(creator, courseId);
        List<UUID> current = store.listSections(courseId).stream().map(Section::id).toList();
        if (!Set.copyOf(current).equals(Set.copyOf(orderedIds)) || current.size() != orderedIds.size()) {
            throw new ValidationException("Order must contain exactly this course's sections");
        }
        store.updateSectionPositions(orderedIds);
    }

    public Lecture updateLecture(User creator, UUID courseId, UUID sectionId, UUID lectureId,
                                 String title, String provider, String rawVideoRef) {
        ownedSection(creator, courseId, sectionId);
        Lecture lecture = lectureInSection(sectionId, lectureId);
        String cleanProvider = blankToNull(provider) == null ? lecture.videoProvider() : provider.trim().toLowerCase();
        String videoId = normalizeVideoId(cleanProvider, requireText(rawVideoRef, "videoId"));
        store.updateLecture(lectureId, requireText(title, "title"), cleanProvider, videoId);
        return store.findLecture(lectureId).orElseThrow();
    }

    public void deleteLecture(User creator, UUID courseId, UUID sectionId, UUID lectureId) {
        ownedSection(creator, courseId, sectionId);
        lectureInSection(sectionId, lectureId);
        store.deleteLecture(lectureId);
    }

    public void reorderLectures(User creator, UUID courseId, UUID sectionId, List<UUID> orderedIds) {
        ownedSection(creator, courseId, sectionId);
        List<UUID> current = store.listLecturesBySection(sectionId).stream().map(Lecture::id).toList();
        if (!Set.copyOf(current).equals(Set.copyOf(orderedIds)) || current.size() != orderedIds.size()) {
            throw new ValidationException("Order must contain exactly this section's lectures");
        }
        store.updateLecturePositions(orderedIds);
    }

    // --- Learner catalog ---

    public List<Course> catalog() {
        return store.listPublished();
    }

    /**
     * Full course detail for viewing. Published courses are visible to anyone; a draft is visible
     * only to its owner.
     */
    public CourseDetail getDetail(User viewer, UUID courseId) {
        Course course = store.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (course.status() != CourseStatus.PUBLISHED && !course.creatorId().equals(viewer.id())) {
            throw new ForbiddenException("Course is not published");
        }
        List<CourseDetail.SectionWithLectures> sections = store.listSections(courseId).stream()
                .map(s -> new CourseDetail.SectionWithLectures(s, store.listLecturesBySection(s.id())))
                .toList();
        return new CourseDetail(course, sections);
    }

    // --- helpers ---

    private void requireCreator(User user) {
        if (user.role() != Role.CREATOR) {
            throw new ForbiddenException("Only creators can author courses");
        }
    }

    private Course getOwnedCourse(User creator, UUID courseId) {
        requireCreator(creator);
        Course course = store.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (!course.creatorId().equals(creator.id())) {
            throw new ForbiddenException("You do not own this course");
        }
        return course;
    }

    private Section ownedSection(User creator, UUID courseId, UUID sectionId) {
        getOwnedCourse(creator, courseId);
        Section section = store.findSection(sectionId)
                .orElseThrow(() -> new NotFoundException("Section not found"));
        if (!section.courseId().equals(courseId)) {
            throw new NotFoundException("Section not found in this course");
        }
        return section;
    }

    private Lecture lectureInSection(UUID sectionId, UUID lectureId) {
        Lecture lecture = store.findLecture(lectureId)
                .orElseThrow(() -> new NotFoundException("Lecture not found"));
        if (!lecture.sectionId().equals(sectionId)) {
            throw new NotFoundException("Lecture not found in this section");
        }
        return lecture;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(field + " is required");
        }
        return value.trim();
    }

    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    /** Accept a YouTube URL or a bare 11-char id; store just the id. */
    static String normalizeVideoId(String provider, String raw) {
        String trimmed = raw.trim();
        if ("youtube".equals(provider)) {
            Matcher m = YOUTUBE.matcher(trimmed);
            if (m.find()) {
                return m.group(1);
            }
            if (trimmed.matches("[A-Za-z0-9_-]{11}")) {
                return trimmed;
            }
            throw new ValidationException("Not a valid YouTube video id or URL");
        }
        return trimmed; // other providers: store as given for now
    }
}
