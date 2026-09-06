package com.coursecraft.domain.object;

import java.util.UUID;

/** The learner's most recent lecture, for a "Continue learning" entry (journey LJ-3). */
public record ContinueItem(
        UUID courseId,
        String courseTitle,
        UUID lectureId,
        String lectureTitle,
        String videoId,
        int positionSec) {
}
