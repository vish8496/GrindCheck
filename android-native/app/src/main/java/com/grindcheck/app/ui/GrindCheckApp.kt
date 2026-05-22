package com.grindcheck.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.grindcheck.app.exercise.ExerciseId
import com.grindcheck.app.ui.theme.NeonGreen
import com.grindcheck.app.ui.workout.WorkoutScreen

@Composable
fun GrindCheckApp() {
    val nav = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        NavHost(navController = nav, startDestination = "home") {
            composable("home") {
                HomeScreen(
                    onExerciseClick = { id -> nav.navigate("workout/${id.name}") }
                )
            }
            composable("workout/{id}") { backStackEntry ->
                val raw = backStackEntry.arguments?.getString("id") ?: ExerciseId.SQUAT.name
                val ex = runCatching { ExerciseId.valueOf(raw) }.getOrDefault(ExerciseId.SQUAT)
                WorkoutScreen(exerciseId = ex, onBack = { nav.popBackStack() })
            }
        }
    }
}

@Composable
private fun HomeScreen(onExerciseClick: (ExerciseId) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        Text(
            text = "GRINDCHECK",
            color = NeonGreen,
            fontSize = 32.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp,
        )
        Text(
            text = "AI form coach. Real-time.",
            color = Color(0xFF9AA19A),
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 4.dp),
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = "PICK YOUR LIFT",
            color = Color(0xFF555555),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 32.dp),
        ) {
            items(ExerciseId.entries) { ex ->
                ExerciseCard(ex = ex, onClick = { onExerciseClick(ex) })
            }
        }
    }
}

@Composable
private fun ExerciseCard(ex: ExerciseId, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF141414), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Column {
            Text(ex.emoji, fontSize = 28.sp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = ex.displayName,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Tap to start",
                color = Color(0xFF555555),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
