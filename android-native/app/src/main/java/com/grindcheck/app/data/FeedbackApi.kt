package com.grindcheck.app.data

import com.grindcheck.app.BuildConfig
import com.grindcheck.app.ui.workout.ClaudeFeedback
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/** Thin Railway POST /feedback client. */
object FeedbackApi {

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
    private val payloadAdapter = moshi.adapter(FeedbackRequest::class.java)
    private val responseAdapter = moshi.adapter(ClaudeFeedback::class.java)

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    data class FeedbackRequest(
        val exercise: String,
        val reps: Int,
        val avgScore: Int,
        val mainErrors: List<String>,
        val worstRep: Int,
    )

    suspend fun get(
        exercise: String,
        reps: Int,
        avgScore: Int,
        mainErrors: List<String>,
        worstRep: Int,
    ): ClaudeFeedback = withContext(Dispatchers.IO) {
        val body = payloadAdapter.toJson(
            FeedbackRequest(exercise, reps, avgScore, mainErrors, worstRep)
        ).toRequestBody("application/json".toMediaType())

        val req = Request.Builder()
            .url("${BuildConfig.API_URL}/feedback")
            .post(body)
            .build()

        client.newCall(req).execute().use { res ->
            val text = res.body?.string() ?: error("Empty response")
            if (!res.isSuccessful) error("HTTP ${res.code}: $text")
            responseAdapter.fromJson(text) ?: error("Could not parse feedback")
        }
    }
}
