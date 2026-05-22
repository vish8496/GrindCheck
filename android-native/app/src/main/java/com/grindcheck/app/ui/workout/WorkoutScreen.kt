package com.grindcheck.app.ui.workout

import android.Manifest
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.grindcheck.app.exercise.ExerciseId
import com.grindcheck.app.pose.PoseAnalyzer
import com.grindcheck.app.ui.theme.NeonGreen
import com.grindcheck.app.ui.theme.TextPrimary
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import java.util.concurrent.Executors

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WorkoutScreen(
    exerciseId: ExerciseId,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraPerm = rememberPermissionState(Manifest.permission.CAMERA)

    LaunchedEffect(Unit) {
        if (!cameraPerm.status.isGranted) cameraPerm.launchPermissionRequest()
    }

    if (!cameraPerm.status.isGranted) {
        PermissionGate(onRequest = { cameraPerm.launchPermissionRequest() }, onBack = onBack)
        return
    }

    // ViewModel factory — passes the exerciseId in
    val vm: WorkoutViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return WorkoutViewModel(exerciseId) as T
            }
        }
    )
    val state by vm.state.collectAsState()

    // Pose analyzer + camera lifecycle
    val analyzer = remember {
        PoseAnalyzer(onPose = { vm.onPose(it) })
    }
    DisposableEffect(Unit) {
        onDispose { analyzer.close() }
    }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(Unit) {
        onDispose { cameraExecutor.shutdown() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Camera preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .setResolutionSelector(
                            ResolutionSelector.Builder()
                                .setResolutionStrategy(
                                    ResolutionStrategy(
                                        Size(640, 480),
                                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                    )
                                )
                                .build()
                        )
                        .build()
                        .also { it.setAnalyzer(cameraExecutor, analyzer) }

                    val selector = CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build()

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                    } catch (_: Exception) {
                        // fallback to rear if front isn't available
                        val rear = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()
                        cameraProvider.bindToLifecycle(lifecycleOwner, rear, preview, imageAnalysis)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Skeleton overlay
        SkeletonOverlay(
            pose = state.pose,
            errorLandmarks = state.errorLandmarks,
            mirrorX = true, // front camera mirror
            modifier = Modifier.fillMaxSize(),
        )

        // Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .height(56.dp)
        ) {
            TextButton(
                onClick = onBack,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White),
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                Text("Back", modifier = Modifier.padding(start = 4.dp))
            }
            Text(
                text = exerciseId.displayName,
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                modifier = Modifier.align(Alignment.Center),
            )
            // FPS counter for dev visibility
            Text(
                text = "${state.fps} fps",
                color = NeonGreen,
                fontSize = 11.sp,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        // Score badge — top right under status bar
        ScoreBadge(
            score = state.liveScore,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(top = 72.dp, end = 16.dp)
        )

        // Rep counter — bottom center
        RepCounter(
            count = state.repCount,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
        )

        // Start / Finish button — bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val detecting = state.status == WorkoutStatus.DETECTING
            Button(
                onClick = {
                    if (detecting) vm.finishSet() else vm.startSet()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (detecting) Color(0xFFFF4444) else NeonGreen,
                    contentColor = Color.Black,
                ),
                shape = RoundedCornerShape(30.dp),
                contentPadding = PaddingValues(horizontal = 48.dp, vertical = 16.dp),
            ) {
                Text(
                    text = if (detecting) "Finish Set" else "Start Set",
                    fontWeight = FontWeight.Black,
                    fontSize = 17.sp,
                )
            }
        }

        // Feedback overlay (modal-ish bottom sheet replacement for now)
        if (state.status == WorkoutStatus.FEEDBACK_LOADING ||
            state.status == WorkoutStatus.FEEDBACK_READY ||
            state.status == WorkoutStatus.ERROR
        ) {
            FeedbackOverlay(state = state, onDone = { vm.nextSet() })
        }
    }
}

@Composable
private fun PermissionGate(onRequest: () -> Unit, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Camera access needed to track your form.",
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
        ) { Text("Grant camera access") }
        TextButton(onClick = onBack) { Text("Back", color = Color(0xFF9AA19A)) }
    }
}
