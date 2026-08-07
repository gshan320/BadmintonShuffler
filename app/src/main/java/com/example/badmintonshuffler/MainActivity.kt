package com.example.badmintonshuffler

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
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
        // The app is dark on every screen and the home screen puts a photograph under the status
        // bar, so the system icons are always drawn light. Left to itself the platform picks dark
        // ones from the light system theme and the clock disappears into the court mat.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            CourtShufflerTheme {
                ErrorBoundary {
                    CourtShufflerApp()
                }
            }
        }
    }
}
