package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/**
 * A scheduled live lecture (journey CJ-7 / LJ-7). MVP uses a pasted YouTube (Live) video id;
 * once ended, the same id serves the auto-archived recording.
 */
public record LiveSession(
        UUID id,
        UUID courseId,
        String title,
        String youtubeVideoId,
        LiveStatus status,
        Instant startsAt,
        Instant createdAt) {
}
