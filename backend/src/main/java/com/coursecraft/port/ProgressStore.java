package com.coursecraft.port;

import com.coursecraft.domain.object.ContinueItem;
import com.coursecraft.domain.object.Progress;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Outbound port for lecture progress (Supabase Postgres via JdbcClient). */
public interface ProgressStore {

    /** Idempotent upsert on (learner, lecture). {@code completed} is sticky once true. */
    void upsert(UUID learnerId, UUID lectureId, int positionSec, boolean completed);

    Optional<Progress> find(UUID learnerId, UUID lectureId);

    /** Progress rows for all lectures of a course, for computing course completion. */
    List<Progress> listByCourse(UUID learnerId, UUID courseId);

    /** The learner's most recently updated lecture, for the continue-learning entry. */
    Optional<ContinueItem> mostRecent(UUID learnerId);
}
