package com.coursecraft.adapter.out.client;

import com.coursecraft.domain.object.Assessment;
import com.coursecraft.domain.object.Attempt;
import com.coursecraft.domain.object.LeaderboardRow;
import com.coursecraft.domain.object.Option;
import com.coursecraft.domain.object.Question;
import com.coursecraft.domain.object.QuestionType;
import com.coursecraft.port.AssessmentStore;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** {@link AssessmentStore} over Supabase Postgres via {@link JdbcClient}. */
public final class JdbcAssessmentStore implements AssessmentStore {

    private final JdbcClient jdbc;

    public JdbcAssessmentStore(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Assessment insertAssessment(Assessment a) {
        jdbc.sql("""
                        INSERT INTO assessment (id, course_id, title, pass_mark, created_at)
                        VALUES (:id, :courseId, :title, :passMark, now())
                        """)
                .param("id", a.id()).param("courseId", a.courseId())
                .param("title", a.title()).param("passMark", a.passMark())
                .update();
        return findAssessment(a.id()).orElseThrow();
    }

    @Override
    public Optional<Assessment> findAssessment(UUID id) {
        return jdbc.sql("SELECT id, course_id, title, pass_mark, created_at FROM assessment WHERE id = :id")
                .param("id", id).query(JdbcAssessmentStore::mapAssessment).optional();
    }

    @Override
    public List<Assessment> listByCourse(UUID courseId) {
        return jdbc.sql("""
                        SELECT id, course_id, title, pass_mark, created_at
                        FROM assessment WHERE course_id = :courseId ORDER BY created_at
                        """)
                .param("courseId", courseId).query(JdbcAssessmentStore::mapAssessment).list();
    }

    @Override
    public Question insertQuestion(Question q) {
        jdbc.sql("""
                        INSERT INTO question (id, assessment_id, text, type, points, position)
                        VALUES (:id, :assessmentId, :text, :type, :points, :position)
                        """)
                .param("id", q.id()).param("assessmentId", q.assessmentId())
                .param("text", q.text()).param("type", q.type().name())
                .param("points", q.points()).param("position", q.position())
                .update();
        return q;
    }

    @Override
    public List<Question> listQuestions(UUID assessmentId) {
        return jdbc.sql("""
                        SELECT id, assessment_id, text, type, points, position
                        FROM question WHERE assessment_id = :assessmentId ORDER BY position
                        """)
                .param("assessmentId", assessmentId).query(JdbcAssessmentStore::mapQuestion).list();
    }

    @Override
    public Option insertOption(Option o) {
        jdbc.sql("""
                        INSERT INTO question_option (id, question_id, text, correct, position)
                        VALUES (:id, :questionId, :text, :correct, :position)
                        """)
                .param("id", o.id()).param("questionId", o.questionId())
                .param("text", o.text()).param("correct", o.correct()).param("position", o.position())
                .update();
        return o;
    }

    @Override
    public List<Option> listOptions(UUID questionId) {
        return jdbc.sql("""
                        SELECT id, question_id, text, correct, position
                        FROM question_option WHERE question_id = :questionId ORDER BY position
                        """)
                .param("questionId", questionId).query(JdbcAssessmentStore::mapOption).list();
    }

    @Override
    public List<Option> listOptionsByAssessment(UUID assessmentId) {
        return jdbc.sql("""
                        SELECT o.id, o.question_id, o.text, o.correct, o.position
                        FROM question_option o
                        JOIN question q ON q.id = o.question_id
                        WHERE q.assessment_id = :assessmentId
                        ORDER BY q.position, o.position
                        """)
                .param("assessmentId", assessmentId).query(JdbcAssessmentStore::mapOption).list();
    }

    @Override
    public void insertAttempt(Attempt a) {
        jdbc.sql("""
                        INSERT INTO attempt (id, assessment_id, learner_id, score, max_score, submitted_at)
                        VALUES (:id, :assessmentId, :learnerId, :score, :maxScore, now())
                        """)
                .param("id", a.id()).param("assessmentId", a.assessmentId())
                .param("learnerId", a.learnerId()).param("score", a.score()).param("maxScore", a.maxScore())
                .update();
    }

    @Override
    public List<LeaderboardRow> leaderboard(UUID assessmentId) {
        // Best score per learner; ties broken by earliest time that best score was reached.
        return jdbc.sql("""
                        SELECT u.id AS learner_id, u.display_name,
                               MAX(a.score) AS best_score,
                               MAX(a.max_score) AS max_score
                        FROM attempt a
                        JOIN app_user u ON u.id = a.learner_id
                        WHERE a.assessment_id = :assessmentId
                        GROUP BY u.id, u.display_name
                        ORDER BY best_score DESC, MIN(a.submitted_at)
                        """)
                .param("assessmentId", assessmentId)
                .query((rs, n) -> new LeaderboardRow(
                        rs.getObject("learner_id", UUID.class),
                        rs.getString("display_name"),
                        rs.getInt("best_score"),
                        rs.getInt("max_score")))
                .list();
    }

    @Override
    public long[] attemptStats(UUID assessmentId) {
        return jdbc.sql("""
                        SELECT count(*) AS attempts,
                               COALESCE(round(avg(CASE WHEN max_score > 0
                                    THEN score * 100.0 / max_score ELSE 0 END)), 0) AS avg_percent
                        FROM attempt WHERE assessment_id = :assessmentId
                        """)
                .param("assessmentId", assessmentId)
                .query((rs, n) -> new long[]{rs.getLong("attempts"), rs.getLong("avg_percent")})
                .single();
    }

    private static Assessment mapAssessment(ResultSet rs, int n) throws SQLException {
        return new Assessment(rs.getObject("id", UUID.class), rs.getObject("course_id", UUID.class),
                rs.getString("title"), rs.getInt("pass_mark"),
                rs.getObject("created_at", OffsetDateTime.class).toInstant());
    }

    private static Question mapQuestion(ResultSet rs, int n) throws SQLException {
        return new Question(rs.getObject("id", UUID.class), rs.getObject("assessment_id", UUID.class),
                rs.getString("text"), QuestionType.valueOf(rs.getString("type")),
                rs.getInt("points"), rs.getInt("position"));
    }

    private static Option mapOption(ResultSet rs, int n) throws SQLException {
        return new Option(rs.getObject("id", UUID.class), rs.getObject("question_id", UUID.class),
                rs.getString("text"), rs.getBoolean("correct"), rs.getInt("position"));
    }
}
