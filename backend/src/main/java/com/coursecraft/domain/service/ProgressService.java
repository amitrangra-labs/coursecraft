package com.coursecraft.domain.service;

import com.coursecraft.domain.object.ContinueItem;
import com.coursecraft.domain.object.Lecture;
import com.coursecraft.domain.object.Progress;
import com.coursecraft.domain.object.User;
import com.coursecraft.port.CourseStore;
import com.coursecraft.port.ProgressStore;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Playback progress (journeys LJ-2/LJ-3). Progress is an idempotent upsert keyed by
 * (learner, lecture); course completion is derived from these rows.
 */
public final class ProgressService {

    private final ProgressStore progress;
    private final CourseStore courses;

    public ProgressService(ProgressStore progress, CourseStore courses) {
        this.progress = progress;
        this.courses = courses;
    }

    public void save(User learner, UUID lectureId, int positionSec, boolean completed) {
        int clamped = Math.max(0, positionSec);
        progress.upsert(learner.id(), lectureId, clamped, completed);
    }

    public Progress get(User learner, UUID lectureId) {
        return progress.find(learner.id(), lectureId)
                .orElse(new Progress(learner.id(), lectureId, 0, false, null));
    }

    /** Percentage of the course's lectures the learner has completed (0-100). */
    public CourseProgress courseProgress(User learner, UUID courseId) {
        List<Lecture> lectures = courses.listLecturesByCourse(courseId);
        int total = lectures.size();
        if (total == 0) {
            return new CourseProgress(0, 0, 0);
        }
        Set<UUID> completedLectureIds = progress.listByCourse(learner.id(), courseId).stream()
                .filter(Progress::completed).map(Progress::lectureId).collect(Collectors.toSet());
        long completed = lectures.stream().filter(l -> completedLectureIds.contains(l.id())).count();
        int percent = (int) (completed * 100 / total);
        return new CourseProgress(percent, (int) completed, total);
    }

    public Optional<ContinueItem> continueLearning(User learner) {
        return progress.mostRecent(learner.id());
    }

    public record CourseProgress(int percent, int completed, int total) {
    }
}
