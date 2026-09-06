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

    private const val TIMEOUT_MS = 10000

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

    suspend fun addLecture(
        token: String, courseId: String, sectionId: String, title: String, videoId: String
    ): LectureView {
        val body = JSONObject()
            .put("title", title)
            .put("videoProvider", "youtube")
            .put("videoId", videoId)
        val path = "/api/courses/$courseId/sections/$sectionId/lectures"
        return parseLecture(JSONObject(request("POST", path, token, body)))
    }

    suspend fun publish(token: String, courseId: String): CourseSummary {
        return parseCourse(JSONObject(request("POST", "/api/courses/$courseId/publish", token, null)))
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
        return CourseDetailView(course, sections)
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
