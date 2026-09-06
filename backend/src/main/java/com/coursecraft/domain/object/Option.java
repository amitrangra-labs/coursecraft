package com.coursecraft.domain.object;

import java.util.UUID;

/** An answer option for a question. {@code correct} is never exposed to a learner before submit. */
public record Option(UUID id, UUID questionId, String text, boolean correct, int position) {
}
