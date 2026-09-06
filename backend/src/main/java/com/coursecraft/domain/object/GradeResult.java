package com.coursecraft.domain.object;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Outcome of grading an attempt, including the correct options revealed after submit. */
public record GradeResult(
        int score,
        int maxScore,
        boolean passed,
        Map<UUID, List<UUID>> correctOptionIdsByQuestion) {
}
