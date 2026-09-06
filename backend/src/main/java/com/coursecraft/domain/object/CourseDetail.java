package com.coursecraft.domain.object;

import java.util.List;

/** A course with its sections and their lectures — the shape a learner views (journey LJ-1). */
public record CourseDetail(Course course, List<SectionWithLectures> sections) {

    public record SectionWithLectures(Section section, List<Lecture> lectures) {
    }
}
