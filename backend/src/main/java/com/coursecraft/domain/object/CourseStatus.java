package com.coursecraft.domain.object;

/** Course lifecycle (journey CJ-5). Learners only ever see PUBLISHED courses. */
public enum CourseStatus {
    DRAFT,
    PUBLISHED,
    ARCHIVED
}
