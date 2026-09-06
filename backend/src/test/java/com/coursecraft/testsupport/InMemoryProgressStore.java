package com.coursecraft.testsupport;

import com.coursecraft.domain.object.ContinueItem;
import com.coursecraft.domain.object.Progress;
import com.coursecraft.port.ProgressStore;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * In-memory {@link ProgressStore} for tests. {@code listByCourse} returns all of a learner's
 * progress; the service intersects it with the course's lectures, so that is sufficient.
 */
public final class InMemoryProgressStore implements ProgressStore {

    private record Key(UUID learner, UUID lecture) {
    }

    private final Map<Key, Progress> rows = new LinkedHashMap<>();

    @Override
    public void upsert(UUID learnerId, UUID lectureId, int positionSec, boolean completed) {
        Key key = new Key(learnerId, lectureId);
        boolean sticky = completed || (rows.containsKey(key) && rows.get(key).completed());
        rows.put(key, new Progress(learnerId, lectureId, positionSec, sticky, Instant.now()));
    }

    @Override
    public Optional<Progress> find(UUID learnerId, UUID lectureId) {
        return Optional.ofNullable(rows.get(new Key(learnerId, lectureId)));
    }

    @Override
    public List<Progress> listByCourse(UUID learnerId, UUID courseId) {
        return rows.values().stream().filter(p -> p.learnerId().equals(learnerId)).toList();
    }

    @Override
    public Optional<ContinueItem> mostRecent(UUID learnerId) {
        return Optional.empty();
    }
}
