package com.grindcheck.app.exercise

import com.grindcheck.app.pose.CONFIDENCE_THRESHOLD
import com.grindcheck.app.pose.KP
import com.grindcheck.app.pose.PoseFrame
import com.grindcheck.app.pose.angleDeg
import kotlin.math.abs

/**
 * Tracks squat reps via knee-angle phase machine.
 *
 * Thresholds are relaxed for MLKit Pose Detection's confidence levels
 * (much higher than MoveNet on horizontal poses — but we keep slack
 * to absorb occasional frame noise).
 */
class SquatDetector : ExerciseDetector {

    private enum class Phase { STANDING, DESCENDING, BOTTOM }
    private var phase: Phase = Phase.STANDING

    private val _reps = mutableListOf<RepScore>()
    override val repScores: List<RepScore> get() = _reps

    override var repCount: Int = 0
        private set
    override var liveScore: Int = 100
        private set
    override var errorLandmarks: List<Int> = emptyList()
        private set

    private var currentRepErrors = mutableSetOf<String>()
    private var deepestKneeAngle = 180f

    private val enterDescending = 155f
    private val enterBottom = 120f
    private val returnTop = 150f

    override fun process(pose: PoseFrame, nowMs: Long): Boolean {
        val lm = pose.landmarks

        val lHip = lm[KP.LEFT_HIP] ?: return false
        val rHip = lm[KP.RIGHT_HIP] ?: return false
        val lKnee = lm[KP.LEFT_KNEE] ?: return false
        val rKnee = lm[KP.RIGHT_KNEE] ?: return false
        val lAnkle = lm[KP.LEFT_ANKLE] ?: return false
        val rAnkle = lm[KP.RIGHT_ANKLE] ?: return false

        val confOk = listOf(lHip, rHip, lKnee, rKnee, lAnkle, rAnkle).all { it.confidence >= CONFIDENCE_THRESHOLD }
        if (!confOk) return false

        // Average left+right for robustness
        val hx = (lHip.x + rHip.x) / 2f
        val hy = (lHip.y + rHip.y) / 2f
        val kx = (lKnee.x + rKnee.x) / 2f
        val ky = (lKnee.y + rKnee.y) / 2f
        val ax = (lAnkle.x + rAnkle.x) / 2f
        val ay = (lAnkle.y + rAnkle.y) / 2f

        val kneeAngle = angleDeg(hx, hy, kx, ky, ax, ay)
        val hipBelowKnee = hy > ky

        var rep = false
        val newErrors = mutableSetOf<String>()

        when (phase) {
            Phase.STANDING -> {
                if (kneeAngle < enterDescending) {
                    phase = Phase.DESCENDING
                    currentRepErrors.clear()
                    deepestKneeAngle = kneeAngle
                }
            }
            Phase.DESCENDING -> {
                deepestKneeAngle = minOf(deepestKneeAngle, kneeAngle)
                if (kneeAngle < enterBottom) phase = Phase.BOTTOM
                else if (kneeAngle > returnTop) {
                    // bounced back up without ever reaching bottom — count as shallow
                    rep = completeRep(setOf("shallow_squat") + currentRepErrors, hipBelowKnee, lKnee, rKnee, lAnkle, rAnkle)
                }
            }
            Phase.BOTTOM -> {
                deepestKneeAngle = minOf(deepestKneeAngle, kneeAngle)
                if (kneeAngle > returnTop) {
                    rep = completeRep(currentRepErrors.toSet(), hipBelowKnee, lKnee, rKnee, lAnkle, rAnkle)
                }
            }
        }

        // Live error highlighting (knee cave during current rep)
        val errorLms = mutableListOf<Int>()
        if (lKnee != null && lAnkle != null && abs(lKnee.x - lAnkle.x) > pose.imageWidth * 0.05f) {
            currentRepErrors.add("knee_cave")
            errorLms += KP.LEFT_KNEE
        }
        if (rKnee != null && rAnkle != null && abs(rKnee.x - rAnkle.x) > pose.imageWidth * 0.05f) {
            currentRepErrors.add("knee_cave")
            errorLms += KP.RIGHT_KNEE
        }
        errorLandmarks = errorLms

        if (rep) {
            val recent = _reps.takeLast(3)
            liveScore = (recent.sumOf { it.score } / recent.size.coerceAtLeast(1)).coerceIn(0, 100)
        }

        return rep
    }

    private fun completeRep(
        errors: Set<String>,
        hipBelowKnee: Boolean,
        lKnee: com.grindcheck.app.pose.Landmark,
        rKnee: com.grindcheck.app.pose.Landmark,
        lAnkle: com.grindcheck.app.pose.Landmark,
        rAnkle: com.grindcheck.app.pose.Landmark,
    ): Boolean {
        val finalErrors = errors.toMutableSet()
        var score = 100

        if (!hipBelowKnee && deepestKneeAngle > 95f) {
            finalErrors += "shallow_squat"
        }
        if (finalErrors.contains("knee_cave")) score -= 15
        if (finalErrors.contains("shallow_squat")) score -= 25
        score = score.coerceIn(0, 100)

        _reps.add(RepScore(score = score, errors = finalErrors.toList()))
        repCount = _reps.size
        phase = Phase.STANDING
        currentRepErrors.clear()
        deepestKneeAngle = 180f
        return true
    }

    override fun reset() {
        phase = Phase.STANDING
        _reps.clear()
        repCount = 0
        liveScore = 100
        errorLandmarks = emptyList()
        currentRepErrors.clear()
        deepestKneeAngle = 180f
    }

    override fun finalize(): SetResult = _reps.computeSetResult()
}
