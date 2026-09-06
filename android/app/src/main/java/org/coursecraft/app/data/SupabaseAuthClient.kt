package org.coursecraft.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.coursecraft.app.BuildConfig
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal email/password auth against Supabase GoTrue — no SDK, just the two REST calls we need.
 * Returns the access token (a JWT) that the backend verifies against the project JWKS.
 */
object SupabaseAuthClient {

    private const val TIMEOUT_MS = 10000

    /** POST /auth/v1/token?grant_type=password — sign an existing user in. */
    suspend fun signIn(email: String, password: String): String =
        authRequest("/auth/v1/token?grant_type=password", email, password)

    /** POST /auth/v1/signup — create a user. Returns a token when email confirmation is off. */
    suspend fun signUp(email: String, password: String): String =
        authRequest("/auth/v1/signup", email, password)

    private suspend fun authRequest(path: String, email: String, password: String): String =
        withContext(Dispatchers.IO) {
            require(BuildConfig.SUPABASE_URL.isNotBlank()) { "SUPABASE_URL not configured" }
            val url = URL("${BuildConfig.SUPABASE_URL}$path")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("apikey", BuildConfig.SUPABASE_PUBLISHABLE_KEY)
            }
            try {
                val body = JSONObject().put("email", email).put("password", password).toString()
                conn.outputStream.use { it.write(body.toByteArray()) }

                val code = conn.responseCode
                val text = (if (code in 200..299) conn.inputStream else conn.errorStream)
                    ?.bufferedReader()?.use { it.readText() } ?: ""

                if (code !in 200..299) {
                    throw RuntimeException(extractError(text) ?: "Auth failed (HTTP $code)")
                }
                val token = JSONObject(text).optString("access_token", "")
                if (token.isBlank()) {
                    // Signup with email confirmation on returns a user but no session.
                    throw RuntimeException("Account created — confirm your email, then sign in.")
                }
                token
            } finally {
                conn.disconnect()
            }
        }

    private fun extractError(json: String): String? = try {
        val obj = JSONObject(json)
        listOf("error_description", "msg", "message", "error")
            .map { obj.optString(it) }
            .firstOrNull { it.isNotBlank() }
    } catch (e: Exception) {
        null
    }
}
