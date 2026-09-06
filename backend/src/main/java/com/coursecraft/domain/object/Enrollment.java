package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/** A learner's enrollment in a course (journey LJ-1). Unique per (learner, course). */
public record Enrollment(UUID id, UUID learnerId, UUID courseId, Instant createdAt) {
}
