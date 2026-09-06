package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/** A graded submission. The leaderboard reads the best attempt per learner (journey LJ-4/LJ-5). */
public record Attempt(UUID id, UUID assessmentId, UUID learnerId, int score, int maxScore, Instant submittedAt) {
}
