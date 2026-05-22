package com.grindcheck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.grindcheck.app.ui.GrindCheckApp
import com.grindcheck.app.ui.theme.GrindCheckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            GrindCheckTheme {
                GrindCheckApp()
            }
        }
    }
}
