package com.coursecraft.domain.object;

import java.util.UUID;

/** One row of an assessment leaderboard: a learner's best score. */
public record LeaderboardRow(UUID learnerId, String displayName, int bestScore, int maxScore) {
}
