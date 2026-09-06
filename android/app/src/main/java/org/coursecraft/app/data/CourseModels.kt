package org.coursecraft.app.data

/** Client-side views of the backend course DTOs. */

data class CourseSummary(
    val id: String,
    val title: String,
    val subject: String?,
    val level: String?,
    val status: String,
    val creatorId: String
)

data class LectureView(
    val id: String,
    val title: String,
    val videoProvider: String?,
    val videoId: String?,
    val position: Int
)

data class SectionView(
    val id: String,
    val title: String,
    val position: Int,
    val lectures: List<LectureView>
)

data class CourseDetailView(
    val course: CourseSummary,
    val sections: List<SectionView>,
    val enrolled: Boolean
)

data class ProgressState(val positionSec: Int, val completed: Boolean)

data class CourseProgress(val percent: Int, val completed: Int, val total: Int)

data class ContinueItem(
    val courseId: String,
    val courseTitle: String,
    val lectureId: String,
    val lectureTitle: String,
    val videoId: String,
    val positionSec: Int
)
