package com.grindcheck.app.ui.workout

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.grindcheck.app.pose.CONFIDENCE_THRESHOLD
import com.grindcheck.app.pose.PoseFrame
import com.grindcheck.app.pose.SKELETON_EDGES

/**
 * Draws an MLKit pose skeleton (33 landmarks → 12 skeletal edges we care about)
 * over the camera preview. Scales pose-coordinate-system to canvas-coordinate-system.
 *
 * The camera is FRONT-FACING so we mirror the X-axis to match the user's reflection.
 */
@Composable
fun SkeletonOverlay(
    pose: PoseFrame?,
    errorLandmarks: List<Int>,
    mirrorX: Boolean,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val p = pose ?: return@Canvas
        if (p.imageWidth <= 0 || p.imageHeight <= 0) return@Canvas

        // The PreviewView uses FILL_CENTER (scale up + crop) so we compute the
        // same transform here so landmarks line up with what's on screen.
        val canvasW = size.width
        val canvasH = size.height
        val imgW = p.imageWidth.toFloat()
        val imgH = p.imageHeight.toFloat()

        val scale = maxOf(canvasW / imgW, canvasH / imgH)
        val scaledW = imgW * scale
        val scaledH = imgH * scale
        val dx = (canvasW - scaledW) / 2f
        val dy = (canvasH - scaledH) / 2f

        fun mapX(x: Float): Float {
            val raw = x * scale + dx
            return if (mirrorX) canvasW - raw else raw
        }
        fun mapY(y: Float): Float = y * scale + dy

        // Edges
        for ((aIdx, bIdx) in SKELETON_EDGES) {
            val a = p.landmarks[aIdx] ?: continue
            val b = p.landmarks[bIdx] ?: continue
            if (a.confidence < CONFIDENCE_THRESHOLD || b.confidence < CONFIDENCE_THRESHOLD) continue
            val isErr = aIdx in errorLandmarks || bIdx in errorLandmarks
            drawLine(
                color = if (isErr) Color(0xFFFF4444) else Color(0xFF00FF87),
                start = Offset(mapX(a.x), mapY(a.y)),
                end = Offset(mapX(b.x), mapY(b.y)),
                strokeWidth = 6f,
                alpha = 0.92f,
            )
        }

        // Keypoints
        for ((idx, lm) in p.landmarks) {
            if (lm.confidence < CONFIDENCE_THRESHOLD) continue
            // skip face landmarks except nose for cleaner overlay
            if (idx > 10 && idx < 11) continue
            val isErr = idx in errorLandmarks
            drawCircle(
                color = if (isErr) Color(0xFFFF4444) else Color(0xFF00FF87),
                radius = 7f,
                center = Offset(mapX(lm.x), mapY(lm.y)),
                alpha = 0.95f,
            )
        }
    }
}
