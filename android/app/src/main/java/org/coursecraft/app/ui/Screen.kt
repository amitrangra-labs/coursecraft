package org.coursecraft.app.ui

/** The in-app destinations. A tiny back stack in CourseCraftApp drives navigation. */
sealed interface Screen {
    data object Home : Screen
    data object Catalog : Screen
    data object MyLearning : Screen
    data object MyCourses : Screen
    data object CreateCourse : Screen
    data class CourseDetail(val courseId: String, val title: String) : Screen
    data class ManageCourse(val courseId: String, val title: String) : Screen
    data class Player(val videoId: String, val title: String, val lectureId: String) : Screen

    // Assessments (Phase 3)
    data class CreatorAssessments(val courseId: String, val title: String) : Screen
    data class AddQuestion(val assessmentId: String, val title: String) : Screen
    data class LearnerAssessments(val courseId: String, val title: String) : Screen
    data class TakeAssessment(val assessmentId: String, val title: String) : Screen
    data class Leaderboard(val assessmentId: String, val title: String) : Screen

    // Live lectures (Phase 3.5)
    data class CreatorLive(val courseId: String, val title: String) : Screen
    data class LearnerLive(val courseId: String, val title: String) : Screen
}
