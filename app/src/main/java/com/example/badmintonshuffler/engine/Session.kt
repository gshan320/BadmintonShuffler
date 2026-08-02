package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.RoundStatus
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.SessionStatus

/**
 * Session lifecycle. Pure Kotlin, so the ViewModel stays a thin wrapper.
 */

/** Whether the setup flow has gathered enough to run a round. */
fun canStartSession(state: SessionState): Boolean =
    state.status == SessionStatus.SETUP &&
        state.activePlayers.size >= com.example.badmintonshuffler.model.SessionDefaults.MIN_PLAYERS &&
        state.config.durationMinutes > 0

/** Flip to active and deal the first round. */
fun startSession(state: SessionState, seed: Int): SessionState {
    if (state.status != SessionStatus.SETUP) return state
    return startNextRound(state.copy(status = SessionStatus.ACTIVE), seed)
}

/**
 * Freeze the session. Results stay readable; nothing more can be recorded.
 */
fun endSession(state: SessionState): SessionState = state.copy(
    status = SessionStatus.ENDED,
    rounds = state.rounds.map { it.copy(status = RoundStatus.COMPLETED) },
)

/**
 * Wipe everything and go back to an empty setup.
 *
 * There is no undo and nothing on disk to recover from — the UI must say so before calling this.
 */
fun clearSession(): SessionState = SessionState()

/** "Next round" stays disabled until every court that still counts has a score. */
fun canAdvanceRound(state: SessionState): Boolean {
    if (state.status != SessionStatus.ACTIVE) return false
    val round = state.currentRound ?: return false
    return round.isFullyScored
}
