package com.coursecraft.domain.object;

/** MVP question types — all auto-gradable (journey CJ-4). */
public enum QuestionType {
    SINGLE,      // one correct option
    MULTIPLE,    // one or more correct options; must match exactly
    TRUE_FALSE   // exactly two options, one correct
}
