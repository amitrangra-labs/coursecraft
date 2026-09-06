package com.coursecraft.domain.object;

import java.util.UUID;

/** A question within an assessment. */
public record Question(UUID id, UUID assessmentId, String text, QuestionType type, int points, int position) {
}
