package org.coursecraft.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.coursecraft.app.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tiny dependency-free client for the CourseCraft backend. Phase 0 only calls the public health
 * endpoint to prove connectivity; authenticated calls (with a Supabase JWT) arrive with sign-in.
 */
object BackendClient {

    private const val TIMEOUT_MS = 8000

    /** GET /api/health — returns the raw JSON body, or throws on failure. */
    suspend fun health(): String = withContext(Dispatchers.IO) {
        val url = URL("${BuildConfig.BACKEND_BASE_URL}/api/health")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        try {
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                throw RuntimeException("HTTP $code: $body")
            }
            body
        } finally {
            conn.disconnect()
        }
    }
}
