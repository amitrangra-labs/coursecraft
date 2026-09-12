package org.coursecraft.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.coursecraft.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Authenticated client for the backend course endpoints (Phase 1). */
object CourseApi {

    private const val TIMEOUT_MS = 30000

    suspend fun becomeCreator(token: String): String {
        val json = JSONObject(request("POST", "/api/creators", token, null))
        return json.optString("role")
    }

    suspend fun createCourse(token: String, title: String, subject: String, level: String): CourseSummary {
        val body = JSONObject().put("title", title).put("subject", subject).put("level", level)
        return parseCourse(JSONObject(request("POST", "/api/courses", token, body)))
    }

    suspend fun addSection(token: String, courseId: String, title: String): SectionView {
        val body = JSONObject().put("title", title)
        return parseSection(JSONObject(request("POST", "/api/courses/$courseId/sections", token, body)))
    }

    /** A direct video URL (not a YouTube link) is stored as a native-playable "mp4" lecture. */
    fun videoProviderFor(ref: String): String {
        val r = ref.trim().lowercase()
        val isYoutube = r.contains("youtube.com") || r.contains("youtu.be")
        return if (r.startsWith("http") && !isYoutube) "mp4" else "youtube"
    }

    suspend fun addLecture(
        token: String, courseId: String, sectionId: String, title: String, videoId: String
    ): LectureView {
        val body = JSONObject()
            .put("title", title)
            .put("videoProvider", videoProviderFor(videoId))
            .put("videoId", videoId)
        val path = "/api/courses/$courseId/sections/$sectionId/lectures"
        return parseLecture(JSONObject(request("POST", path, token, body)))
    }

    suspend fun publish(token: String, courseId: String): CourseSummary {
        return parseCourse(JSONObject(request("POST", "/api/courses/$courseId/publish", token, null)))
    }

    // --- editing / reordering ---

    suspend fun renameCourse(token: String, courseId: String, title: String, subject: String, level: String) {
        val body = JSONObject().put("title", title).put("subject", subject).put("level", level)
        request("PUT", "/api/courses/$courseId", token, body)
    }

    suspend fun deleteCourse(token: String, courseId: String) {
        request("DELETE", "/api/courses/$courseId", token, null)
    }

    suspend fun renameSection(token: String, courseId: String, sectionId: String, title: String) {
        val body = JSONObject().put("title", title)
        request("PUT", "/api/courses/$courseId/sections/$sectionId", token, body)
    }

    suspend fun deleteSection(token: String, courseId: String, sectionId: String) {
        request("DELETE", "/api/courses/$courseId/sections/$sectionId", token, null)
    }

    suspend fun reorderSections(token: String, courseId: String, orderedIds: List<String>) {
        val body = JSONObject().put("ids", JSONArray(orderedIds))
        request("PUT", "/api/courses/$courseId/sections/order", token, body)
    }

    suspend fun updateLecture(
        token: String, courseId: String, sectionId: String, lectureId: String, title: String, videoId: String
    ) {
        val body = JSONObject().put("title", title)
            .put("videoProvider", videoProviderFor(videoId)).put("videoId", videoId)
        request("PUT", "/api/courses/$courseId/sections/$sectionId/lectures/$lectureId", token, body)
    }

    suspend fun deleteLecture(token: String, courseId: String, sectionId: String, lectureId: String) {
        request("DELETE", "/api/courses/$courseId/sections/$sectionId/lectures/$lectureId", token, null)
    }

    suspend fun reorderLectures(token: String, courseId: String, sectionId: String, orderedIds: List<String>) {
        val body = JSONObject().put("ids", JSONArray(orderedIds))
        request("PUT", "/api/courses/$courseId/sections/$sectionId/lectures/order", token, body)
    }

    suspend fun myCourses(token: String): List<CourseSummary> =
        parseCourseList(request("GET", "/api/courses/mine", token, null))

    suspend fun catalog(token: String): List<CourseSummary> =
        parseCourseList(request("GET", "/api/catalog", token, null))

    suspend fun courseDetail(token: String, courseId: String): CourseDetailView {
        val json = JSONObject(request("GET", "/api/courses/$courseId", token, null))
        val course = parseCourse(json.getJSONObject("course"))
        val sections = json.getJSONArray("sections").let { arr ->
            (0 until arr.length()).map { parseSection(arr.getJSONObject(it)) }
        }
        return CourseDetailView(course, sections, json.optBoolean("enrolled"))
    }

    suspend fun myLearning(token: String): List<CourseSummary> =
        parseCourseList(request("GET", "/api/my/learning", token, null))

    suspend fun enroll(token: String, courseId: String) {
        request("POST", "/api/courses/$courseId/enroll", token, null)
    }

    suspend fun unenroll(token: String, courseId: String) {
        request("DELETE", "/api/courses/$courseId/enroll", token, null)
    }

    // --- progress (LJ-2 / LJ-3) ---

    suspend fun saveProgress(token: String, lectureId: String, positionSec: Int, completed: Boolean) {
        val body = JSONObject().put("lectureId", lectureId).put("positionSec", positionSec).put("completed", completed)
        request("PUT", "/api/progress", token, body)
    }

    suspend fun getProgress(token: String, lectureId: String): ProgressState {
        val o = JSONObject(request("GET", "/api/lectures/$lectureId/progress", token, null))
        return ProgressState(o.optInt("positionSec"), o.optBoolean("completed"))
    }

    suspend fun courseProgress(token: String, courseId: String): CourseProgress {
        val o = JSONObject(request("GET", "/api/courses/$courseId/progress", token, null))
        return CourseProgress(o.optInt("percent"), o.optInt("completed"), o.optInt("total"))
    }

    suspend fun updateDisplayName(token: String, displayName: String) {
        val body = JSONObject().put("displayName", displayName)
        request("PUT", "/api/me/display-name", token, body)
    }

    suspend fun courseAnalytics(token: String, courseId: String): CourseAnalytics {
        val o = JSONObject(request("GET", "/api/courses/$courseId/analytics", token, null))
        val stats = o.getJSONArray("assessments").let { arr ->
            (0 until arr.length()).map {
                val a = arr.getJSONObject(it)
                AssessmentStat(a.getString("title"), a.optInt("attempts"), a.optInt("averagePercent"))
            }
        }
        return CourseAnalytics(o.optInt("enrollments"), stats)
    }

    suspend fun continueLearning(token: String): ContinueItem? {
        val o = JSONObject(request("GET", "/api/my/continue", token, null))
        if (o.optBoolean("none")) return null
        return ContinueItem(
            o.getString("courseId"), o.getString("courseTitle"),
            o.getString("lectureId"), o.getString("lectureTitle"),
            o.optString("videoId"), o.optInt("positionSec")
        )
    }

    // --- HTTP ---

    private suspend fun request(method: String, path: String, token: String, body: JSONObject?): String =
        withContext(Dispatchers.IO) {
            val conn = (URL("${BuildConfig.BACKEND_BASE_URL}$path").openConnection() as HttpURLConnection)
                .apply {
                    requestMethod = method
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("Authorization", "Bearer $token")
                    if (body != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                    }
                }
            try {
                if (body != null) conn.outputStream.use { it.write(body.toString().toByteArray()) }
                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""
                if (code !in 200..299) throw RuntimeException(errorMessage(text) ?: "HTTP $code")
                text
            } finally {
                conn.disconnect()
            }
        }

    private fun errorMessage(json: String): String? = try {
        JSONObject(json).optString("message").takeIf { it.isNotEmpty() }
    } catch (e: Exception) {
        null
    }

    // --- parsing ---

    private fun parseCourseList(json: String): List<CourseSummary> {
        val arr = JSONArray(json)
        return (0 until arr.length()).map { parseCourse(arr.getJSONObject(it)) }
    }

    private fun parseCourse(o: JSONObject) = CourseSummary(
        id = o.getString("id"),
        title = o.getString("title"),
        subject = o.optString("subject").takeIf { it.isNotEmpty() },
        level = o.optString("level").takeIf { it.isNotEmpty() },
        status = o.getString("status"),
        creatorId = o.getString("creatorId")
    )

    private fun parseSection(o: JSONObject): SectionView {
        val lectures = o.optJSONArray("lectures")?.let { arr ->
            (0 until arr.length()).map { parseLecture(arr.getJSONObject(it)) }
        } ?: emptyList()
        return SectionView(
            id = o.getString("id"),
            title = o.getString("title"),
            position = o.optInt("position"),
            lectures = lectures
        )
    }

    private fun parseLecture(o: JSONObject) = LectureView(
        id = o.getString("id"),
        title = o.getString("title"),
        videoProvider = o.optString("videoProvider").takeIf { it.isNotEmpty() },
        videoId = o.optString("videoId").takeIf { it.isNotEmpty() },
        position = o.optInt("position")
    )
}
