package com.grindcheck.app.pose

import com.google.mlkit.vision.pose.PoseLandmark

/** Lightweight representation of a frame's detected pose. */
data class PoseFrame(
    val landmarks: Map<Int, Landmark>,
    val imageWidth: Int,
    val imageHeight: Int,
)

/** Single keypoint with normalized image coords and detection confidence. */
data class Landmark(
    val type: Int,
    val x: Float,       // pixel x in image
    val y: Float,       // pixel y in image
    val confidence: Float,
)

/** MLKit Pose landmark indices, aliased for our exercise logic.
 *  MLKit gives 33 landmarks (BlazePose). We mostly care about these. */
object KP {
    const val NOSE = PoseLandmark.NOSE
    const val LEFT_SHOULDER = PoseLandmark.LEFT_SHOULDER
    const val RIGHT_SHOULDER = PoseLandmark.RIGHT_SHOULDER
    const val LEFT_ELBOW = PoseLandmark.LEFT_ELBOW
    const val RIGHT_ELBOW = PoseLandmark.RIGHT_ELBOW
    const val LEFT_WRIST = PoseLandmark.LEFT_WRIST
    const val RIGHT_WRIST = PoseLandmark.RIGHT_WRIST
    const val LEFT_HIP = PoseLandmark.LEFT_HIP
    const val RIGHT_HIP = PoseLandmark.RIGHT_HIP
    const val LEFT_KNEE = PoseLandmark.LEFT_KNEE
    const val RIGHT_KNEE = PoseLandmark.RIGHT_KNEE
    const val LEFT_ANKLE = PoseLandmark.LEFT_ANKLE
    const val RIGHT_ANKLE = PoseLandmark.RIGHT_ANKLE
}

/** Edges between landmarks for drawing the skeleton overlay. */
val SKELETON_EDGES: List<Pair<Int, Int>> = listOf(
    KP.LEFT_SHOULDER to KP.RIGHT_SHOULDER,
    KP.LEFT_SHOULDER to KP.LEFT_ELBOW,
    KP.LEFT_ELBOW to KP.LEFT_WRIST,
    KP.RIGHT_SHOULDER to KP.RIGHT_ELBOW,
    KP.RIGHT_ELBOW to KP.RIGHT_WRIST,
    KP.LEFT_SHOULDER to KP.LEFT_HIP,
    KP.RIGHT_SHOULDER to KP.RIGHT_HIP,
    KP.LEFT_HIP to KP.RIGHT_HIP,
    KP.LEFT_HIP to KP.LEFT_KNEE,
    KP.LEFT_KNEE to KP.LEFT_ANKLE,
    KP.RIGHT_HIP to KP.RIGHT_KNEE,
    KP.RIGHT_KNEE to KP.RIGHT_ANKLE,
)

const val CONFIDENCE_THRESHOLD = 0.5f

/** Angle in degrees at vertex B formed by A–B–C. */
fun angleDeg(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Float {
    val radians = Math.atan2((cy - by).toDouble(), (cx - bx).toDouble()) -
        Math.atan2((ay - by).toDouble(), (ax - bx).toDouble())
    var angle = Math.abs(Math.toDegrees(radians))
    if (angle > 180.0) angle = 360.0 - angle
    return angle.toFloat()
}
