package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/** A course authored by a creator (journeys CJ-2, CJ-5). */
public record Course(
        UUID id,
        UUID creatorId,
        String title,
        String subject,
        String level,
        CourseStatus status,
        Instant createdAt) {
}
