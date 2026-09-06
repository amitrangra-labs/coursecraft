package org.coursecraft.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.coursecraft.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Authenticated client for the assessment endpoints (Phase 3). */
object AssessmentApi {

    private const val TIMEOUT_MS = 10000

    suspend fun listForCourse(token: String, courseId: String): List<AssessmentSummary> {
        val arr = JSONArray(request("GET", "/api/courses/$courseId/assessments", token, null))
        return (0 until arr.length()).map { parseSummary(arr.getJSONObject(it)) }
    }

    suspend fun createAssessment(token: String, courseId: String, title: String, passMark: Int): AssessmentSummary {
        val body = JSONObject().put("title", title).put("passMark", passMark)
        return parseSummary(JSONObject(request("POST", "/api/courses/$courseId/assessments", token, body)))
    }

    /** options: list of (text, correct). */
    suspend fun addQuestion(
        token: String, assessmentId: String, text: String, type: String, points: Int,
        options: List<Pair<String, Boolean>>
    ) {
        val optsArr = JSONArray()
        options.forEach { optsArr.put(JSONObject().put("text", it.first).put("correct", it.second)) }
        val body = JSONObject().put("text", text).put("type", type).put("points", points).put("options", optsArr)
        request("POST", "/api/assessments/$assessmentId/questions", token, body)
    }

    suspend fun take(token: String, assessmentId: String): TakeView {
        val o = JSONObject(request("GET", "/api/assessments/$assessmentId/take", token, null))
        val questions = o.getJSONArray("questions").let { arr ->
            (0 until arr.length()).map { parseTakeQuestion(arr.getJSONObject(it)) }
        }
        return TakeView(o.getString("id"), o.getString("title"), o.optInt("passMark"), questions)
    }

    /** answers: questionId -> selected optionIds. */
    suspend fun submit(token: String, assessmentId: String, answers: Map<String, List<String>>): SubmitResult {
        val answersArr = JSONArray()
        answers.forEach { (qid, optIds) ->
            answersArr.put(JSONObject().put("questionId", qid).put("optionIds", JSONArray(optIds)))
        }
        val body = JSONObject().put("answers", answersArr)
        val o = JSONObject(request("POST", "/api/assessments/$assessmentId/submit", token, body))
        val correct = mutableMapOf<String, List<String>>()
        o.getJSONArray("correct").let { arr ->
            for (i in 0 until arr.length()) {
                val c = arr.getJSONObject(i)
                val ids = c.getJSONArray("optionIds").let { a -> (0 until a.length()).map { a.getString(it) } }
                correct[c.getString("questionId")] = ids
            }
        }
        return SubmitResult(o.getInt("score"), o.getInt("maxScore"), o.getBoolean("passed"), correct)
    }

    suspend fun leaderboard(token: String, assessmentId: String): List<LeaderboardEntry> {
        val arr = JSONArray(request("GET", "/api/assessments/$assessmentId/leaderboard", token, null))
        return (0 until arr.length()).map {
            val o = arr.getJSONObject(it)
            LeaderboardEntry(o.getString("displayName"), o.getInt("bestScore"), o.getInt("maxScore"))
        }
    }

    private fun parseSummary(o: JSONObject) = AssessmentSummary(o.getString("id"), o.getString("title"), o.optInt("passMark"))

    private fun parseTakeQuestion(o: JSONObject): TakeQuestion {
        val options = o.getJSONArray("options").let { arr ->
            (0 until arr.length()).map { TakeOption(arr.getJSONObject(it).getString("id"), arr.getJSONObject(it).getString("text")) }
        }
        return TakeQuestion(o.getString("id"), o.getString("text"), o.getString("type"), o.optInt("points"), options)
    }

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
