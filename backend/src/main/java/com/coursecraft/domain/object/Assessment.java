package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/** An assessment attached to a course. {@code passMark} is a percentage (0-100). */
public record Assessment(UUID id, UUID courseId, String title, int passMark, Instant createdAt) {
}
