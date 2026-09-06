package org.coursecraft.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.coursecraft.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LiveSession(
    val id: String,
    val title: String,
    val videoId: String,
    val status: String, // SCHEDULED | LIVE | ENDED | CANCELED
    val startsAtEpochMs: Long
)

/** Authenticated client for live-lecture endpoints (Phase 3.5). */
object LiveApi {

    private const val TIMEOUT_MS = 10000

    suspend fun listForCourse(token: String, courseId: String): List<LiveSession> {
        val arr = JSONArray(request("GET", "/api/courses/$courseId/live", token, null))
        return (0 until arr.length()).map { parse(arr.getJSONObject(it)) }
    }

    suspend fun schedule(token: String, courseId: String, title: String, videoId: String, startsAtEpochMs: Long): LiveSession {
        val body = JSONObject().put("title", title).put("youtubeVideoId", videoId).put("startsAtEpochMs", startsAtEpochMs)
        return parse(JSONObject(request("POST", "/api/courses/$courseId/live", token, body)))
    }

    suspend fun goLive(token: String, sessionId: String) = act(token, sessionId, "start")
    suspend fun end(token: String, sessionId: String) = act(token, sessionId, "end")
    suspend fun cancel(token: String, sessionId: String) = act(token, sessionId, "cancel")

    private suspend fun act(token: String, sessionId: String, action: String): LiveSession =
        parse(JSONObject(request("POST", "/api/live/$sessionId/$action", token, null)))

    private fun parse(o: JSONObject) = LiveSession(
        o.getString("id"), o.getString("title"), o.optString("videoId"),
        o.getString("status"), o.optLong("startsAtEpochMs")
    )

    private suspend fun request(method: String, path: String, token: String, body: JSONObject?): String =
        withContext(Dispatchers.IO) {
            val conn = (URL("${BuildConfig.BACKEND_BASE_URL}$path").openConnection() as HttpURLConnection).apply {
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
                if (code !in 200..299) {
                    val msg = try { JSONObject(text).optString("message").takeIf { it.isNotEmpty() } } catch (e: Exception) { null }
                    throw RuntimeException(msg ?: "HTTP $code")
                }
                text
            } finally {
                conn.disconnect()
            }
        }
}
