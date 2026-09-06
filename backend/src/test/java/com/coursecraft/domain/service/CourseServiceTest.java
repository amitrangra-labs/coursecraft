package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.Section;
import com.coursecraft.domain.object.User;
import com.coursecraft.testsupport.InMemoryCourseStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pure domain tests for authoring rules — no Spring, no DB. */
class CourseServiceTest {

    private final CourseService service = new CourseService(new InMemoryCourseStore());

    private static User user(Role role) {
        return new User(UUID.randomUUID(), "sub-" + role, role.name().toLowerCase() + "@x.io",
                role.name().toLowerCase(), role, Instant.now());
    }

    @Test
    void learnerCannotCreateCourse() {
        assertThrows(ForbiddenException.class,
                () -> service.createCourse(user(Role.LEARNER), "Algebra", "Math", "Beginner"));
    }

    @Test
    void createCourseStartsAsDraft() {
        Course c = service.createCourse(user(Role.CREATOR), "Algebra", "Math", "Beginner");
        assertEquals(CourseStatus.DRAFT, c.status());
        assertEquals("Algebra", c.title());
    }

    @Test
    void cannotEditAnotherCreatorsCourse() {
        User owner = user(Role.CREATOR);
        Course c = service.createCourse(owner, "Algebra", "Math", "Beginner");
        User other = user(Role.CREATOR);
        assertThrows(ForbiddenException.class, () -> service.addSection(other, c.id(), "Intro"));
    }

    @Test
    void publishRequiresSectionAndLecture() {
        User owner = user(Role.CREATOR);
        Course c = service.createCourse(owner, "Algebra", "Math", "Beginner");

        ValidationException noSection = assertThrows(ValidationException.class,
                () -> service.publish(owner, c.id()));
        assertTrue(noSection.getMessage().contains("section"));

        Section s = service.addSection(owner, c.id(), "Intro");
        assertThrows(ValidationException.class, () -> service.publish(owner, c.id()));

        service.addVideoLecture(owner, c.id(), s.id(), "Welcome", "youtube", "dQw4w9WgXcQ");
        Course published = service.publish(owner, c.id());
        assertEquals(CourseStatus.PUBLISHED, published.status());
    }

    @Test
    void addLectureNormalizesYoutubeUrl() {
        User owner = user(Role.CREATOR);
        Course c = service.createCourse(owner, "Algebra", "Math", "Beginner");
        Section s = service.addSection(owner, c.id(), "Intro");

        Lecture l = service.addVideoLecture(owner, c.id(), s.id(), "Welcome", "youtube",
                "https://www.youtube.com/watch?v=dQw4w9WgXcQ");
        assertEquals("dQw4w9WgXcQ", l.videoId());
    }

    @Test
    void renameAndDeleteSectionAndLecture() {
        User owner = user(Role.CREATOR);
        Course c = service.createCourse(owner, "Algebra", "Math", "Beginner");
        Section s = service.addSection(owner, c.id(), "Intro");
        var l = service.addVideoLecture(owner, c.id(), s.id(), "Welcome", "youtube", "dQw4w9WgXcQ");

        Section renamed = service.renameSection(owner, c.id(), s.id(), "Getting started");
        assertEquals("Getting started", renamed.title());

        var updated = service.updateLecture(owner, c.id(), s.id(), l.id(), "Intro video", "youtube", "abcdEFGHijk");
        assertEquals("Intro video", updated.title());
        assertEquals("abcdEFGHijk", updated.videoId());

        service.deleteLecture(owner, c.id(), s.id(), l.id());
        assertEquals(0, service.getDetail(owner, c.id()).sections().get(0).lectures().size());

        service.deleteSection(owner, c.id(), s.id());
        assertEquals(0, service.getDetail(owner, c.id()).sections().size());
    }

    @Test
    void reorderSectionsChangesOrder() {
        User owner = user(Role.CREATOR);
        Course c = service.createCourse(owner, "Algebra", "Math", "Beginner");
        Section a = service.addSection(owner, c.id(), "A");
        Section b = service.addSection(owner, c.id(), "B");

        service.reorderSections(owner, c.id(), java.util.List.of(b.id(), a.id()));

        var detail = service.getDetail(owner, c.id());
        assertEquals("B", detail.sections().get(0).section().title());
        assertEquals("A", detail.sections().get(1).section().title());
    }

    @Test
    void reorderRejectsForeignIds() {
        User owner = user(Role.CREATOR);
        Course c = service.createCourse(owner, "Algebra", "Math", "Beginner");
        service.addSection(owner, c.id(), "A");
        assertThrows(ValidationException.class,
                () -> service.reorderSections(owner, c.id(), java.util.List.of(UUID.randomUUID())));
    }

    @Test
    void publishedCourseAppearsInCatalog() {
        User owner = user(Role.CREATOR);
        Course c = service.createCourse(owner, "Algebra", "Math", "Beginner");
        Section s = service.addSection(owner, c.id(), "Intro");
        service.addVideoLecture(owner, c.id(), s.id(), "Welcome", "youtube", "dQw4w9WgXcQ");
        service.publish(owner, c.id());

        assertEquals(1, service.catalog().size());
    }
}
