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
