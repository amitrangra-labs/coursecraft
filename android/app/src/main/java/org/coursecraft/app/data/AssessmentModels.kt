package org.coursecraft.app.data

data class AssessmentSummary(val id: String, val title: String, val passMark: Int)

data class TakeOption(val id: String, val text: String)

data class TakeQuestion(
    val id: String,
    val text: String,
    val type: String, // SINGLE | MULTIPLE | TRUE_FALSE
    val points: Int,
    val options: List<TakeOption>
)

data class TakeView(
    val id: String,
    val title: String,
    val passMark: Int,
    val questions: List<TakeQuestion>
)

data class SubmitResult(
    val score: Int,
    val maxScore: Int,
    val passed: Boolean,
    val correct: Map<String, List<String>>
)

data class LeaderboardEntry(val displayName: String, val bestScore: Int, val maxScore: Int)
