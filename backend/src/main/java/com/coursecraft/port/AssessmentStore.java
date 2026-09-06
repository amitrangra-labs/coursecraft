package com.coursecraft.port;

import com.coursecraft.domain.object.Assessment;
import com.coursecraft.domain.object.Attempt;
import com.coursecraft.domain.object.LeaderboardRow;
import com.coursecraft.domain.object.Option;
import com.coursecraft.domain.object.Question;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for assessments, questions, options, attempts, and leaderboards. */
public interface AssessmentStore {

    Assessment insertAssessment(Assessment assessment);

    Optional<Assessment> findAssessment(UUID id);

    List<Assessment> listByCourse(UUID courseId);

    Question insertQuestion(Question question);

    List<Question> listQuestions(UUID assessmentId);

    Option insertOption(Option option);

    List<Option> listOptions(UUID questionId);

    /** All options for every question in the assessment — used for grading. */
    List<Option> listOptionsByAssessment(UUID assessmentId);

    void insertAttempt(Attempt attempt);

    /** Best score per learner, highest first (ties broken by earliest submission). */
    List<LeaderboardRow> leaderboard(UUID assessmentId);
}
