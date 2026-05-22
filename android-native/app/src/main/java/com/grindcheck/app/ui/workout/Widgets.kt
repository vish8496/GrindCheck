package com.grindcheck.app.ui.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.grindcheck.app.ui.theme.NeonGreen

@Composable
fun ScoreBadge(score: Int, modifier: Modifier = Modifier) {
    val color = when {
        score >= 85 -> NeonGreen
        score >= 60 -> Color(0xFFFFC107)
        else -> Color(0xFFFF4444)
    }
    Box(
        modifier = modifier
            .background(Color(0xCC0A0A0A), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$score",
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "FORM",
                color = Color(0xFF9AA19A),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun RepCounter(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color(0xCC0A0A0A), RoundedCornerShape(24.dp))
            .padding(horizontal = 24.dp, vertical = 12.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$count",
                color = NeonGreen,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "REPS",
                color = Color(0xFF9AA19A),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 0.dp),
            )
        }
    }
}

@Composable
fun FeedbackOverlay(state: WorkoutUiState, onDone: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE60A0A0A)),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (state.status) {
                WorkoutStatus.FEEDBACK_LOADING -> {
                    CircularProgressIndicator(color = NeonGreen)
                    Spacer(Modifier.height(16.dp))
                    Text("Asking Claude...", color = Color.White, fontSize = 16.sp)
                }
                WorkoutStatus.FEEDBACK_READY -> {
                    state.setResult?.let { result ->
                        Text(
                            text = "${result.reps.size} REPS · ${result.avgScore}/100",
                            color = NeonGreen,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                    state.feedback?.let { fb ->
                        FeedbackLine(label = "MISTAKE", text = fb.mistake, color = Color(0xFFFF4444))
                        Spacer(Modifier.height(12.dp))
                        FeedbackLine(label = "FIX", text = fb.fix, color = NeonGreen)
                        Spacer(Modifier.height(12.dp))
                        FeedbackLine(label = "VIBE", text = fb.vibe, color = Color(0xFFFFC107))
                        Spacer(Modifier.height(24.dp))
                    }
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Next Set", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    }
                }
                WorkoutStatus.ERROR -> {
                    Text(
                        text = state.errorMessage ?: "Something went wrong",
                        color = Color(0xFFFF4444),
                        fontSize = 14.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onDone,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                    ) {
                        Text("Try Again", fontWeight = FontWeight.Black)
                    }
                }
                else -> Unit
            }
        }
    }
}

@Composable
private fun FeedbackLine(label: String, text: String, color: Color) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Black)
        Text(text, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
