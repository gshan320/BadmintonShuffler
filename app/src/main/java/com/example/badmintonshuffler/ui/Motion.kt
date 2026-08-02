package com.example.badmintonshuffler.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import android.app.Activity
import android.view.WindowManager

/**
 * True when the user has asked the system to reduce or remove animation.
 *
 * The results podium is the only place in this app that animates for effect, and it is exactly the
 * kind of motion that makes some people feel ill. Honouring this is not optional.
 */
@Composable
fun rememberReduceMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}

/**
 * Hold the screen awake for as long as this composable is on screen.
 *
 * The organiser's phone spends the afternoon face-up on a bench between games. A screen that has
 * locked itself is a screen someone has to unlock with sweaty hands while three courts wait.
 */
@Composable
fun KeepScreenOn() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}
