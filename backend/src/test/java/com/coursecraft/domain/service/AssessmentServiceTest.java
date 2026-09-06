package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Assessment;
import com.coursecraft.domain.object.AssessmentDetail;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.GradeResult;
import com.coursecraft.domain.object.Question;
import com.coursecraft.domain.object.QuestionType;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.Section;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.service.AssessmentService.OptionDraft;
import com.coursecraft.testsupport.InMemoryAssessmentStore;
import com.coursecraft.testsupport.InMemoryCourseStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AssessmentServiceTest {

    private final InMemoryCourseStore courseStore = new InMemoryCourseStore();
    private final CourseService courseService = new CourseService(courseStore);
    private final AssessmentService service = new AssessmentService(new InMemoryAssessmentStore(), courseStore);

    private static User user(Role role) {
        return new User(UUID.randomUUID(), "sub-" + UUID.randomUUID(), "u@x.io", "u", role, Instant.now());
    }

    private Course publishedCourse(User creator) {
        Course c = courseService.createCourse(creator, "Algebra", "Math", "Beginner");
        Section s = courseService.addSection(creator, c.id(), "Intro");
        courseService.addVideoLecture(creator, c.id(), s.id(), "Welcome", "youtube", "dQw4w9WgXcQ");
        return courseService.publish(creator, c.id());
    }

    @Test
    void learnerCannotAuthorAssessment() {
        Course c = publishedCourse(user(Role.CREATOR));
        assertThrows(ForbiddenException.class,
                () -> service.createAssessment(user(Role.LEARNER), c.id(), "Quiz", 50));
    }

    @Test
    void singleChoiceNeedsExactlyOneCorrect() {
        User owner = user(Role.CREATOR);
        Course c = publishedCourse(owner);
        Assessment a = service.createAssessment(owner, c.id(), "Quiz", 50);
        assertThrows(ValidationException.class, () -> service.addQuestion(owner, a.id(), "2+2?",
                QuestionType.SINGLE, 1, List.of(new OptionDraft("4", true), new OptionDraft("5", true))));
    }

    @Test
    void gradesAndRanksAttempts() {
        User owner = user(Role.CREATOR);
        Course c = publishedCourse(owner);
        Assessment a = service.createAssessment(owner, c.id(), "Quiz", 50);
        Question q = service.addQuestion(owner, a.id(), "2+2?", QuestionType.SINGLE, 10,
                List.of(new OptionDraft("4", true), new OptionDraft("5", false)));

        // fetch the correct option id via the detail
        AssessmentDetail detail = service.getDetail(owner, a.id());
        UUID correctOptionId = detail.questions().get(0).options().stream()
                .filter(o -> o.text().equals("4")).findFirst().orElseThrow().id();
        UUID wrongOptionId = detail.questions().get(0).options().stream()
                .filter(o -> o.text().equals("5")).findFirst().orElseThrow().id();

        User good = user(Role.LEARNER);
        User bad = user(Role.LEARNER);

        GradeResult goodResult = service.submit(good, a.id(), Map.of(q.id(), Set.of(correctOptionId)));
        assertEquals(10, goodResult.score());
        assertEquals(10, goodResult.maxScore());
        assertTrue(goodResult.passed());

        GradeResult badResult = service.submit(bad, a.id(), Map.of(q.id(), Set.of(wrongOptionId)));
        assertEquals(0, badResult.score());
        assertFalse(badResult.passed());

        var board = service.leaderboard(owner, a.id());
        assertEquals(2, board.size());
        assertEquals(10, board.get(0).bestScore()); // highest first
    }
}
