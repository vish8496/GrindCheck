package com.grindcheck.app.pose

import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions

/**
 * CameraX ImageAnalysis.Analyzer that pipes each camera frame through MLKit's
 * Pose Detector, then hands the result to a callback.
 *
 * Uses STREAM_MODE (low latency, optimized for real-time).
 */
class PoseAnalyzer(
    private val onPose: (PoseFrame) -> Unit,
) : ImageAnalysis.Analyzer {

    private val detector = PoseDetection.getClient(
        PoseDetectorOptions.Builder()
            .setDetectorMode(PoseDetectorOptions.STREAM_MODE)
            .build()
    )

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        val rotation = imageProxy.imageInfo.rotationDegrees
        val input = InputImage.fromMediaImage(mediaImage, rotation)

        // Rotation-aware image size: when rotation is 90/270 we swap W/H so the
        // landmark pixel coords match what the overlay will draw on a portrait surface.
        val imgW = if (rotation == 90 || rotation == 270) mediaImage.height else mediaImage.width
        val imgH = if (rotation == 90 || rotation == 270) mediaImage.width else mediaImage.height

        detector.process(input)
            .addOnSuccessListener { pose ->
                val map = HashMap<Int, Landmark>(33)
                for (lm in pose.allPoseLandmarks) {
                    map[lm.landmarkType] = Landmark(
                        type = lm.landmarkType,
                        x = lm.position.x,
                        y = lm.position.y,
                        confidence = lm.inFrameLikelihood,
                    )
                }
                onPose(PoseFrame(landmarks = map, imageWidth = imgW, imageHeight = imgH))
            }
            .addOnCompleteListener { imageProxy.close() }
    }

    fun close() {
        detector.close()
    }
}
