package com.coursecraft.adapter.in.endpoint;

import com.coursecraft.adapter.in.auth.RequestAuth;
import com.coursecraft.domain.object.Assessment;
import com.coursecraft.domain.object.AssessmentDetail;
import com.coursecraft.domain.object.GradeResult;
import com.coursecraft.domain.object.LeaderboardRow;
import com.coursecraft.domain.object.Question;
import com.coursecraft.domain.object.QuestionType;
import com.coursecraft.domain.object.User;
import com.coursecraft.domain.service.AssessmentService;
import com.coursecraft.domain.service.AssessmentService.OptionDraft;
import com.coursecraft.domain.service.ProfileService;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Assessment authoring, taking, grading, and leaderboards (journeys CJ-4, LJ-4, LJ-5). */
public final class AssessmentHandler {

    private final ProfileService profileService;
    private final AssessmentService assessmentService;

    public AssessmentHandler(ProfileService profileService, AssessmentService assessmentService) {
        this.profileService = profileService;
        this.assessmentService = assessmentService;
    }

    // --- creator ---

    public ServerResponse create(ServerRequest request) throws Exception {
        User user = currentUser(request);
        CreateAssessmentRequest body = request.body(CreateAssessmentRequest.class);
        Assessment a = assessmentService.createAssessment(user, uuid(request, "courseId"),
                body.title(), body.passMark() == null ? 50 : body.passMark());
        return ServerResponse.ok().body(AssessmentSummary.from(a));
    }

    public ServerResponse addQuestion(ServerRequest request) throws Exception {
        User user = currentUser(request);
        AddQuestionRequest body = request.body(AddQuestionRequest.class);
        List<OptionDraft> options = body.options() == null ? List.of() : body.options().stream()
                .map(o -> new OptionDraft(o.text(), Boolean.TRUE.equals(o.correct()))).toList();
        Question q = assessmentService.addQuestion(user, uuid(request, "assessmentId"),
                body.text(), QuestionType.valueOf(body.type()),
                body.points() == null ? 1 : body.points(), options);
        return ServerResponse.ok().body(Map.of("id", q.id().toString()));
    }

    public ServerResponse listForCourse(ServerRequest request) {
        User user = currentUser(request);
        return ServerResponse.ok().body(assessmentService.listForCourse(user, uuid(request, "courseId"))
                .stream().map(AssessmentSummary::from).toList());
    }

    // --- learner ---

    public ServerResponse take(ServerRequest request) {
        User user = currentUser(request);
        AssessmentDetail detail = assessmentService.getDetail(user, uuid(request, "assessmentId"));
        return ServerResponse.ok().body(TakeView.from(detail));
    }

    public ServerResponse submit(ServerRequest request) throws Exception {
        User user = currentUser(request);
        SubmitRequest body = request.body(SubmitRequest.class);
        Map<UUID, Set<UUID>> answers = new HashMap<>();
        if (body.answers() != null) {
            for (AnswerInput a : body.answers()) {
                Set<UUID> ids = new HashSet<>();
                if (a.optionIds() != null) {
                    a.optionIds().forEach(id -> ids.add(UUID.fromString(id)));
                }
                answers.put(UUID.fromString(a.questionId()), ids);
            }
        }
        GradeResult result = assessmentService.submit(user, uuid(request, "assessmentId"), answers);
        return ServerResponse.ok().body(SubmitResult.from(result));
    }

    public ServerResponse leaderboard(ServerRequest request) {
        User user = currentUser(request);
        List<LeaderboardRow> rows = assessmentService.leaderboard(user, uuid(request, "assessmentId"));
        return ServerResponse.ok().body(rows.stream()
                .map(r -> new LeaderboardEntry(r.displayName(), r.bestScore(), r.maxScore())).toList());
    }

    // --- helpers ---

    private User currentUser(ServerRequest request) {
        return profileService.getOrProvision(RequestAuth.tokenOf(request));
    }

    private static UUID uuid(ServerRequest request, String name) {
        return UUID.fromString(request.pathVariable(name));
    }

    // --- request bodies ---

    public record CreateAssessmentRequest(String title, Integer passMark) {
    }

    public record OptionInput(String text, Boolean correct) {
    }

    public record AddQuestionRequest(String text, String type, Integer points, List<OptionInput> options) {
    }

    public record AnswerInput(String questionId, List<String> optionIds) {
    }

    public record SubmitRequest(List<AnswerInput> answers) {
    }

    // --- responses ---

    public record AssessmentSummary(String id, String title, int passMark) {
        static AssessmentSummary from(Assessment a) {
            return new AssessmentSummary(a.id().toString(), a.title(), a.passMark());
        }
    }

    public record TakeOption(String id, String text) {
    }

    public record TakeQuestion(String id, String text, String type, int points, List<TakeOption> options) {
        static TakeQuestion from(AssessmentDetail.QuestionWithOptions qo) {
            Question q = qo.question();
            List<TakeOption> opts = qo.options().stream()
                    .map(o -> new TakeOption(o.id().toString(), o.text())).toList();
            return new TakeQuestion(q.id().toString(), q.text(), q.type().name(), q.points(), opts);
        }
    }

    public record TakeView(String id, String title, int passMark, List<TakeQuestion> questions) {
        static TakeView from(AssessmentDetail detail) {
            Assessment a = detail.assessment();
            List<TakeQuestion> qs = detail.questions().stream().map(TakeQuestion::from).toList();
            return new TakeView(a.id().toString(), a.title(), a.passMark(), qs);
        }
    }

    public record CorrectAnswer(String questionId, List<String> optionIds) {
    }

    public record SubmitResult(int score, int maxScore, boolean passed, List<CorrectAnswer> correct) {
        static SubmitResult from(GradeResult r) {
            List<CorrectAnswer> correct = r.correctOptionIdsByQuestion().entrySet().stream()
                    .map(e -> new CorrectAnswer(e.getKey().toString(),
                            e.getValue().stream().map(UUID::toString).toList()))
                    .toList();
            return new SubmitResult(r.score(), r.maxScore(), r.passed(), correct);
        }
    }

    public record LeaderboardEntry(String displayName, int bestScore, int maxScore) {
    }
}
