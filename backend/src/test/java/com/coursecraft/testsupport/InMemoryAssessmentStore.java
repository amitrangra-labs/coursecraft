package com.coursecraft.testsupport;

import com.coursecraft.domain.object.Assessment;
import com.coursecraft.domain.object.Attempt;
import com.coursecraft.domain.object.LeaderboardRow;
import com.coursecraft.domain.object.Option;
import com.coursecraft.domain.object.Question;
import com.coursecraft.port.AssessmentStore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** In-memory {@link AssessmentStore} for tests. */
public final class InMemoryAssessmentStore implements AssessmentStore {

    private final Map<UUID, Assessment> assessments = new LinkedHashMap<>();
    private final Map<UUID, Question> questions = new LinkedHashMap<>();
    private final Map<UUID, Option> options = new LinkedHashMap<>();
    private final List<Attempt> attempts = new ArrayList<>();

    @Override
    public Assessment insertAssessment(Assessment a) {
        assessments.put(a.id(), a);
        return a;
    }

    @Override
    public Optional<Assessment> findAssessment(UUID id) {
        return Optional.ofNullable(assessments.get(id));
    }

    @Override
    public List<Assessment> listByCourse(UUID courseId) {
        return assessments.values().stream().filter(a -> a.courseId().equals(courseId)).toList();
    }

    @Override
    public Question insertQuestion(Question q) {
        questions.put(q.id(), q);
        return q;
    }

    @Override
    public List<Question> listQuestions(UUID assessmentId) {
        return questions.values().stream()
                .filter(q -> q.assessmentId().equals(assessmentId))
                .sorted(Comparator.comparingInt(Question::position)).toList();
    }

    @Override
    public Option insertOption(Option o) {
        options.put(o.id(), o);
        return o;
    }

    @Override
    public List<Option> listOptions(UUID questionId) {
        return options.values().stream()
                .filter(o -> o.questionId().equals(questionId))
                .sorted(Comparator.comparingInt(Option::position)).toList();
    }

    @Override
    public List<Option> listOptionsByAssessment(UUID assessmentId) {
        List<UUID> qids = listQuestions(assessmentId).stream().map(Question::id).toList();
        return options.values().stream().filter(o -> qids.contains(o.questionId())).toList();
    }

    @Override
    public void insertAttempt(Attempt a) {
        attempts.add(a);
    }

    @Override
    public List<LeaderboardRow> leaderboard(UUID assessmentId) {
        Map<UUID, Attempt> bestByLearner = new LinkedHashMap<>();
        for (Attempt a : attempts) {
            if (!a.assessmentId().equals(assessmentId)) continue;
            bestByLearner.merge(a.learnerId(), a, (x, y) -> y.score() > x.score() ? y : x);
        }
        return bestByLearner.values().stream()
                .sorted(Comparator.comparingInt(Attempt::score).reversed())
                .map(a -> new LeaderboardRow(a.learnerId(), a.learnerId().toString(), a.score(), a.maxScore()))
                .toList();
    }
}
