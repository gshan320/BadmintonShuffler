package com.example.badmintonshuffler

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.badmintonshuffler.ui.CourtShufflerApp
import com.example.badmintonshuffler.ui.ErrorBoundary
import com.example.badmintonshuffler.ui.theme.CourtShufflerTheme

/**
 * The only Activity. Everything else is Compose.
 *
 * The session lives in a ViewModel scoped to this Activity, so a rotation or a keyboard resize does
 * not lose the afternoon. Process death does — that is by design, see SPEC.md.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CourtShufflerTheme {
                ErrorBoundary {
                    CourtShufflerApp()
                }
            }
        }
    }
}
