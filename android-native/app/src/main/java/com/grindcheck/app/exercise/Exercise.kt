package com.grindcheck.app.exercise

import com.grindcheck.app.pose.PoseFrame

enum class ExerciseId(val displayName: String, val emoji: String) {
    SQUAT("Squats", "🏋️"),
    PUSHUP("Push-ups", "💪"),
    PLANK("Plank", "🥺"),
    DEADLIFT("Deadlift", "🥵"),
    CURL("Bicep Curl", "💪"),
}

data class RepScore(
    val score: Int,
    val errors: List<String>,
)

data class SetResult(
    val avgScore: Int,
    val reps: List<RepScore>,
    val mainErrors: List<String>,
    val worstRepIndex: Int,
)

/** Common interface for exercise rep detection. */
interface ExerciseDetector {
    /** Process one pose frame; returns true if a new rep just completed. */
    fun process(pose: PoseFrame, nowMs: Long): Boolean

    val repCount: Int
    val liveScore: Int           // 0..100, current quality estimate
    val errorLandmarks: List<Int> // landmark indices to highlight red
    val repScores: List<RepScore>

    fun reset()
    fun finalize(): SetResult
}

fun List<RepScore>.computeSetResult(): SetResult {
    if (isEmpty()) return SetResult(avgScore = 0, reps = emptyList(), mainErrors = emptyList(), worstRepIndex = 0)
    val avg = (sumOf { it.score } / size).coerceIn(0, 100)
    val counts = HashMap<String, Int>()
    for (r in this) for (e in r.errors) counts[e] = (counts[e] ?: 0) + 1
    val mainErrors = counts.entries.sortedByDescending { it.value }.take(3).map { it.key }
    val worstIdx = indices.minByOrNull { this[it].score } ?: 0
    return SetResult(avgScore = avg, reps = this, mainErrors = mainErrors, worstRepIndex = worstIdx)
}
