package com.coursecraft.domain.object;

import java.time.Instant;
import java.util.UUID;

/**
 * A lecture within a section. For MVP video lectures we store only the external provider + video
 * id (YouTube/Vimeo) — the bytes live at the provider, never in our system.
 */
public record Lecture(
        UUID id,
        UUID sectionId,
        String title,
        LectureType type,
        String videoProvider,
        String videoId,
        int position,
        Instant createdAt) {
}
