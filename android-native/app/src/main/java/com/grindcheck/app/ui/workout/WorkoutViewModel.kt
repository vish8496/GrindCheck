package com.grindcheck.app.ui.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grindcheck.app.exercise.ExerciseDetector
import com.grindcheck.app.exercise.ExerciseId
import com.grindcheck.app.exercise.SetResult
import com.grindcheck.app.exercise.SquatDetector
import com.grindcheck.app.pose.PoseFrame
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class WorkoutStatus { IDLE, DETECTING, FINISHED, FEEDBACK_LOADING, FEEDBACK_READY, ERROR }

data class WorkoutUiState(
    val status: WorkoutStatus = WorkoutStatus.IDLE,
    val pose: PoseFrame? = null,
    val repCount: Int = 0,
    val liveScore: Int = 100,
    val fps: Int = 0,
    val errorLandmarks: List<Int> = emptyList(),
    val setResult: SetResult? = null,
    val feedback: ClaudeFeedback? = null,
    val errorMessage: String? = null,
)

data class ClaudeFeedback(val mistake: String, val fix: String, val vibe: String)

class WorkoutViewModel(
    private val exerciseId: ExerciseId,
) : ViewModel() {

    private val detector: ExerciseDetector = when (exerciseId) {
        ExerciseId.SQUAT -> SquatDetector()
        else -> SquatDetector() // Phase 1: only squat. Others added in Phase 4.
    }

    private val _state = MutableStateFlow(WorkoutUiState())
    val state: StateFlow<WorkoutUiState> = _state.asStateFlow()

    // fps counter — increment frameCount on every pose, sample once per second
    private var frameCount = 0
    private var lastFpsSample = System.currentTimeMillis()

    fun onPose(pose: PoseFrame) {
        val now = System.currentTimeMillis()
        frameCount++
        if (now - lastFpsSample >= 1000) {
            val fps = frameCount
            frameCount = 0
            lastFpsSample = now
            _state.update { it.copy(fps = fps) }
        }

        if (_state.value.status == WorkoutStatus.DETECTING) {
            detector.process(pose, now)
        }

        _state.update {
            it.copy(
                pose = pose,
                repCount = detector.repCount,
                liveScore = detector.liveScore,
                errorLandmarks = detector.errorLandmarks,
            )
        }
    }

    fun startSet() {
        detector.reset()
        _state.update { it.copy(status = WorkoutStatus.DETECTING, repCount = 0, liveScore = 100, setResult = null, feedback = null) }
    }

    fun finishSet() {
        val result = detector.finalize()
        _state.update { it.copy(status = WorkoutStatus.FEEDBACK_LOADING, setResult = result) }
        viewModelScope.launch {
            fetchFeedback(result)
        }
    }

    private suspend fun fetchFeedback(result: SetResult) {
        try {
            val feedback = com.grindcheck.app.data.FeedbackApi.get(
                exercise = exerciseId.displayName.removeSuffix("s"),
                reps = result.reps.size,
                avgScore = result.avgScore,
                mainErrors = result.mainErrors,
                worstRep = result.worstRepIndex,
            )
            _state.update { it.copy(status = WorkoutStatus.FEEDBACK_READY, feedback = feedback) }
        } catch (t: Throwable) {
            _state.update { it.copy(status = WorkoutStatus.ERROR, errorMessage = t.message ?: "Feedback unavailable") }
        }
    }

    fun nextSet() {
        detector.reset()
        _state.update { it.copy(status = WorkoutStatus.IDLE, setResult = null, feedback = null, repCount = 0, liveScore = 100) }
    }
}
