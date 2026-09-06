package com.coursecraft.domain.object;

import java.util.List;

/** An assessment with its questions and options. */
public record AssessmentDetail(Assessment assessment, List<QuestionWithOptions> questions) {

    public record QuestionWithOptions(Question question, List<Option> options) {
    }
}
