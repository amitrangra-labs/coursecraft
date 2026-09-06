package com.coursecraft.domain.service;

import com.coursecraft.domain.error.DomainExceptions.ForbiddenException;
import com.coursecraft.domain.error.DomainExceptions.NotFoundException;
import com.coursecraft.domain.error.DomainExceptions.ValidationException;
import com.coursecraft.domain.object.Assessment;
import com.coursecraft.domain.object.AssessmentDetail;
import com.coursecraft.domain.object.Attempt;
import com.coursecraft.domain.object.Course;
import com.coursecraft.domain.object.CourseStatus;
import com.coursecraft.domain.object.GradeResult;
import com.coursecraft.domain.object.LeaderboardRow;
import com.coursecraft.domain.object.Option;
import com.coursecraft.domain.object.Question;
import com.coursecraft.domain.object.QuestionType;
import com.coursecraft.domain.object.Role;
import com.coursecraft.domain.object.User;
import com.coursecraft.port.AssessmentStore;
import com.coursecraft.port.CourseStore;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assessment authoring + auto-grading (journeys CJ-4, LJ-4, LJ-5). Grading and access are
 * server-authoritative; correct answers are never returned before submit.
 */
public final class AssessmentService {

    private final AssessmentStore store;
    private final CourseStore courses;

    public AssessmentService(AssessmentStore store, CourseStore courses) {
        this.store = store;
        this.courses = courses;
    }

    // --- authoring ---

    public Assessment createAssessment(User creator, UUID courseId, String title, int passMark) {
        ownedCourse(creator, courseId);
        if (title == null || title.isBlank()) {
            throw new ValidationException("title is required");
        }
        if (passMark < 0 || passMark > 100) {
            throw new ValidationException("passMark must be 0-100");
        }
        return store.insertAssessment(new Assessment(UUID.randomUUID(), courseId, title.trim(), passMark, null));
    }

    public Question addQuestion(User creator, UUID assessmentId, String text, QuestionType type,
                                int points, List<OptionDraft> options) {
        Assessment assessment = ownedAssessment(creator, assessmentId);
        if (text == null || text.isBlank()) {
            throw new ValidationException("question text is required");
        }
        if (points <= 0) {
            throw new ValidationException("points must be positive");
        }
        validateOptions(type, options);

        int position = store.listQuestions(assessment.id()).size();
        Question question = store.insertQuestion(
                new Question(UUID.randomUUID(), assessment.id(), text.trim(), type, points, position));
        for (int i = 0; i < options.size(); i++) {
            OptionDraft o = options.get(i);
            store.insertOption(new Option(UUID.randomUUID(), question.id(), o.text().trim(), o.correct(), i));
        }
        return question;
    }

    private static void validateOptions(QuestionType type, List<OptionDraft> options) {
        if (options == null || options.size() < 2) {
            throw new ValidationException("a question needs at least two options");
        }
        if (options.stream().anyMatch(o -> o.text() == null || o.text().isBlank())) {
            throw new ValidationException("option text is required");
        }
        long correct = options.stream().filter(OptionDraft::correct).count();
        switch (type) {
            case SINGLE -> {
                if (correct != 1) throw new ValidationException("single-choice needs exactly one correct option");
            }
            case TRUE_FALSE -> {
                if (options.size() != 2) throw new ValidationException("true/false needs exactly two options");
                if (correct != 1) throw new ValidationException("true/false needs exactly one correct option");
            }
            case MULTIPLE -> {
                if (correct < 1) throw new ValidationException("multiple-choice needs at least one correct option");
            }
        }
    }

    // --- viewing ---

    public List<Assessment> listForCourse(User viewer, UUID courseId) {
        requireVisibleCourse(viewer, courseId);
        return store.listByCourse(courseId);
    }

    /** Full detail (options include the correct flag). Handlers strip it for the learner take view. */
    public AssessmentDetail getDetail(User viewer, UUID assessmentId) {
        Assessment assessment = findAssessment(assessmentId);
        requireVisibleCourse(viewer, assessment.courseId());
        Map<UUID, List<Option>> optionsByQuestion = store.listOptionsByAssessment(assessmentId).stream()
                .collect(Collectors.groupingBy(Option::questionId));
        List<AssessmentDetail.QuestionWithOptions> questions = store.listQuestions(assessmentId).stream()
                .map(q -> new AssessmentDetail.QuestionWithOptions(
                        q, optionsByQuestion.getOrDefault(q.id(), List.of())))
                .toList();
        return new AssessmentDetail(assessment, questions);
    }

    // --- taking ---

    public GradeResult submit(User learner, UUID assessmentId, Map<UUID, Set<UUID>> answers) {
        Assessment assessment = findAssessment(assessmentId);
        requireVisibleCourse(learner, assessment.courseId());

        List<Question> questions = store.listQuestions(assessmentId);
        Map<UUID, List<Option>> optionsByQuestion = store.listOptionsByAssessment(assessmentId).stream()
                .collect(Collectors.groupingBy(Option::questionId));

        int score = 0;
        int maxScore = 0;
        Map<UUID, List<UUID>> correctByQuestion = new java.util.HashMap<>();
        for (Question q : questions) {
            maxScore += q.points();
            Set<UUID> correctIds = optionsByQuestion.getOrDefault(q.id(), List.of()).stream()
                    .filter(Option::correct).map(Option::id).collect(Collectors.toSet());
            correctByQuestion.put(q.id(), List.copyOf(correctIds));
            Set<UUID> selected = answers.getOrDefault(q.id(), Set.of());
            if (!correctIds.isEmpty() && correctIds.equals(selected)) {
                score += q.points();
            }
        }
        boolean passed = maxScore > 0 && (score * 100 / maxScore) >= assessment.passMark();
        store.insertAttempt(new Attempt(UUID.randomUUID(), assessmentId, learner.id(), score, maxScore, null));
        return new GradeResult(score, maxScore, passed, correctByQuestion);
    }

    public List<LeaderboardRow> leaderboard(User viewer, UUID assessmentId) {
        Assessment assessment = findAssessment(assessmentId);
        requireVisibleCourse(viewer, assessment.courseId());
        return store.leaderboard(assessmentId);
    }

    // --- helpers ---

    private Assessment findAssessment(UUID id) {
        return store.findAssessment(id).orElseThrow(() -> new NotFoundException("Assessment not found"));
    }

    private Course ownedCourse(User creator, UUID courseId) {
        if (creator.role() != Role.CREATOR) {
            throw new ForbiddenException("Only creators can author assessments");
        }
        Course course = courses.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (!course.creatorId().equals(creator.id())) {
            throw new ForbiddenException("You do not own this course");
        }
        return course;
    }

    private Assessment ownedAssessment(User creator, UUID assessmentId) {
        Assessment assessment = findAssessment(assessmentId);
        ownedCourse(creator, assessment.courseId());
        return assessment;
    }

    /** A course is visible if it is published, or the viewer owns it. */
    private void requireVisibleCourse(User viewer, UUID courseId) {
        Course course = courses.findCourse(courseId)
                .orElseThrow(() -> new NotFoundException("Course not found"));
        if (course.status() != CourseStatus.PUBLISHED && !course.creatorId().equals(viewer.id())) {
            throw new ForbiddenException("Course is not published");
        }
    }

    /** Input draft for an option when authoring a question. */
    public record OptionDraft(String text, boolean correct) {
    }
}
