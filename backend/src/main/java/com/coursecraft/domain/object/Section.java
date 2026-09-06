package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/** An ordered grouping of lectures within a course. */
public record Section(
        UUID id,
        UUID courseId,
        String title,
        int position,
        Instant createdAt) {
}
