package com.example.badmintonshuffler.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.badmintonshuffler.model.Match
import com.example.badmintonshuffler.model.SessionStatus
import com.example.badmintonshuffler.state.SessionViewModel
import com.example.badmintonshuffler.ui.component.ConfirmDialog
import com.example.badmintonshuffler.ui.component.COURT_DRAWN_MS
import com.example.badmintonshuffler.ui.component.CourtLoadingOverlay
import com.example.badmintonshuffler.ui.screen.FairnessBreakdownDialog
import com.example.badmintonshuffler.ui.screen.HomeScreen
import com.example.badmintonshuffler.ui.screen.LeaderboardScreen
import com.example.badmintonshuffler.ui.screen.ResultsScreen
import com.example.badmintonshuffler.ui.screen.RosterSheet
import com.example.badmintonshuffler.ui.screen.ScoreEntrySheet
import com.example.badmintonshuffler.ui.screen.SessionScreen
import com.example.badmintonshuffler.ui.screen.SetupCourtsScreen
import com.example.badmintonshuffler.ui.screen.SetupPaceScreen
import com.example.badmintonshuffler.ui.screen.SetupPlayersScreen
import com.example.badmintonshuffler.ui.screen.SetupScoringScreen
import com.example.badmintonshuffler.ui.screen.SetupTimeScreen
import kotlinx.coroutines.delay

/**
 * Why the court is being drawn, and what to call it while it is.
 *
 * Both moments are the rotation engine picking a draw — the only two places in the app where the
 * answer on screen is genuinely new — which is why these are the only two that get the animation.
 */
private enum class DrawingCourts(val label: String?) {
    NONE(null),
    ENTERING("Setting the courts"),
    NEXT_ROUND("Drawing the next round"),
}

/**
 * The whole screen flow. A forward-only setup stack, then a session that owns the rest.
 */
object Routes {
    const val HOME = "home"
    const val SETUP_COURTS = "setup/courts"
    const val SETUP_TIME = "setup/time"
    const val SETUP_PACE = "setup/pace"
    const val SETUP_SCORING = "setup/scoring"
    const val SETUP_PLAYERS = "setup/players"
    const val SESSION = "session"
    const val LEADERBOARD = "session/leaderboard"
    const val RESULTS = "session/results"
}

@Composable
fun CourtShufflerApp(
    viewModel: SessionViewModel = viewModel(),
    navController: NavHostController = rememberNavController(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by viewModel.sessionProgress.collectAsStateWithLifecycle()
    val fairness by viewModel.fairness.collectAsStateWithLifecycle()
    val leaderboard by viewModel.leaderboard.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val canAdvance by viewModel.canAdvance.collectAsStateWithLifecycle()
    val reduceMotion = rememberReduceMotion()

    NavHost(navController = navController, startDestination = Routes.HOME) {

        composable(Routes.HOME) {
            HomeScreen(
                onNewSession = {
                    viewModel.clearSession()
                    navController.navigate(Routes.SETUP_COURTS)
                },
            )
        }

        // --- Setup wizard ---

        composable(Routes.SETUP_COURTS) {
            SetupCourtsScreen(
                config = state.config,
                onCourtCountChange = { count -> viewModel.setConfig { copy(courtCount = count) } },
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SETUP_TIME) },
            )
        }

        composable(Routes.SETUP_TIME) {
            SetupTimeScreen(
                config = state.config,
                onTimesChange = { start, end ->
                    viewModel.setConfig { copy(startTime = start, endTime = end) }
                },
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SETUP_PACE) },
            )
        }

        composable(Routes.SETUP_PACE) {
            SetupPaceScreen(
                config = state.config,
                onMinutesChange = { m -> viewModel.setConfig { copy(minutesPerGame = m) } },
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SETUP_SCORING) },
            )
        }

        composable(Routes.SETUP_SCORING) {
            SetupScoringScreen(
                config = state.config,
                onTargetChange = { t -> viewModel.setConfig { copy(targetScore = t) } },
                onPointsPerWinChange = { p -> viewModel.setConfig { copy(pointsPerWin = p) } },
                onPointsPerLossChange = { p -> viewModel.setConfig { copy(pointsPerLoss = p) } },
                onBack = { navController.popBackStack() },
                onNext = { navController.navigate(Routes.SETUP_PLAYERS) },
            )
        }

        composable(Routes.SETUP_PLAYERS) {
            SetupPlayersScreen(
                config = state.config,
                players = state.players,
                onAddPlayer = viewModel::addPlayer,
                onRemovePlayer = viewModel::removePlayerDuringSetup,
                onBack = { navController.popBackStack() },
                onStart = {
                    viewModel.startSession()
                    navController.navigate(Routes.SESSION) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }

        // --- Live session ---

        composable(Routes.SESSION) {
            var openMatch by remember { mutableStateOf<Match?>(null) }
            var rosterOpen by remember { mutableStateOf(false) }
            var fairnessOpen by remember { mutableStateOf(false) }
            var confirmingEnd by remember { mutableStateOf(false) }
            var confirmingExit by remember { mutableStateOf(false) }

            // A draw appearing instantly reads as though nothing was decided. This is remembered
            // against the nav entry, so it plays on the way in from setup and on each new round —
            // not every time the leaderboard is closed.
            var drawing by remember { mutableStateOf(DrawingCourts.ENTERING) }

            // Nothing is persisted, so backing out of a live session is a destructive act.
            BackHandler(enabled = state.status == SessionStatus.ACTIVE) {
                confirmingExit = true
            }

            LaunchedEffect(drawing) {
                if (drawing != DrawingCourts.NONE) {
                    delay(COURT_DRAWN_MS)
                    drawing = DrawingCourts.NONE
                }
            }

            Box(modifier = Modifier.fillMaxSize()) {
                SessionScreen(
                    state = state,
                    progress = progress,
                    fairness = fairness,
                    canAdvance = canAdvance,
                    onOpenMatch = { openMatch = it },
                    onNextRound = {
                        viewModel.generateNextRound()
                        drawing = DrawingCourts.NEXT_ROUND
                    },
                    onEditPlayers = { rosterOpen = true },
                    onOpenFairness = { fairnessOpen = true },
                    onOpenLeaderboard = { navController.navigate(Routes.LEADERBOARD) },
                    onEndSession = { confirmingEnd = true },
                )

                // Someone who has asked for less motion has also asked not to be made to wait for
                // an animation, so they get the draw immediately instead of a still court.
                if (!reduceMotion) {
                    drawing.label?.let { CourtLoadingOverlay(label = it) }
                }
            }

            openMatch?.let { match ->
                // Re-read from state so the sheet always reflects the latest scores.
                val live = state.match(match.id) ?: match
                ScoreEntrySheet(
                    state = state,
                    match = live,
                    onConfirm = { a, b -> viewModel.recordResult(live.id, a, b) },
                    onVoid = { viewModel.voidMatch(live.id) },
                    onDismiss = { openMatch = null },
                )
            }

            if (rosterOpen) {
                RosterSheet(
                    state = state,
                    onAddPlayer = viewModel::addPlayer,
                    onRemovePlayer = viewModel::removePlayer,
                    onSubstitute = viewModel::substitutePlayer,
                    onVoidMatch = viewModel::voidMatch,
                    onDismiss = { rosterOpen = false },
                )
            }

            if (fairnessOpen) {
                FairnessBreakdownDialog(
                    entries = leaderboard,
                    report = fairness,
                    onDismiss = { fairnessOpen = false },
                )
            }

            if (confirmingEnd) {
                ConfirmDialog(
                    title = "End the session?",
                    body = "This ends the session. No more rounds and no more scores. " +
                        "You can still see the results.",
                    confirmText = "End session",
                    onConfirm = {
                        confirmingEnd = false
                        viewModel.endSession()
                        navController.navigate(Routes.RESULTS)
                    },
                    onDismiss = { confirmingEnd = false },
                )
            }

            if (confirmingExit) {
                ConfirmDialog(
                    title = "Leave the session?",
                    body = "Nothing is saved anywhere. Going back now loses this afternoon's " +
                        "rounds and scores for good.",
                    confirmText = "Leave and lose it",
                    dismissText = "Stay",
                    destructive = true,
                    onConfirm = {
                        confirmingExit = false
                        viewModel.clearSession()
                        navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                    },
                    onDismiss = { confirmingExit = false },
                )
            }
        }

        composable(Routes.LEADERBOARD) {
            LeaderboardScreen(
                entries = leaderboard,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.RESULTS) {
            ResultsScreen(
                entries = leaderboard,
                stats = stats,
                onClearSession = {
                    viewModel.clearSession()
                    navController.navigate(Routes.HOME) { popUpTo(Routes.HOME) { inclusive = true } }
                },
            )
        }
    }
}
