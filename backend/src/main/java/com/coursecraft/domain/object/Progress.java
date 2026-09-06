package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/**
 * A learner's playback progress on one lecture (journey LJ-2/LJ-3). Unique per
 * (learner, lecture) — the idempotent upsert target that powers resume + completion.
 */
public record Progress(
        UUID learnerId,
        UUID lectureId,
        int positionSec,
        boolean completed,
        Instant updatedAt) {
}
