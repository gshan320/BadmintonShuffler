package com.example.badmintonshuffler.engine

import com.example.badmintonshuffler.model.LateJoinerMode
import com.example.badmintonshuffler.model.MatchStatus
import com.example.badmintonshuffler.model.Player
import com.example.badmintonshuffler.model.SessionState
import com.example.badmintonshuffler.model.SessionStatus

/**
 * Adding, removing and substituting players — at any point in a session. Pure Kotlin.
 */

/** The outcome of removing a player, plus whether the organiser has a decision to make right now. */
data class RemovalOutcome(
    val state: SessionState,
    /** True when the player is in a pending match of the current round, so the UI must ask. */
    val affectsCurrentRound: Boolean,
    /** The match they are stuck in, if any. */
    val affectedMatchId: String?,
)

/**
 * Internal player ids are sequential rather than random.
 *
 * Players are never deleted, only deactivated, so the list only ever grows and this can't collide.
 * Being deterministic makes engine tests reproducible, and the id is never shown to anyone.
 */
fun nextPlayerId(state: SessionState): String = "p${state.players.size + 1}"

/**
 * Make [name] unique within the session by suffixing a counter, so a second Alex becomes "Alex (2)".
 */
fun uniqueName(state: SessionState, name: String): String {
    val trimmed = name.trim()
    val taken = state.players.map { it.name.lowercase() }.toSet()
    if (trimmed.lowercase() !in taken) return trimmed
    var suffix = 2
    while ("$trimmed ($suffix)".lowercase() in taken) suffix++
    return "$trimmed ($suffix)"
}

/**
 * Add a player, now or halfway through the afternoon.
 *
 * During setup this is trivial. Once the session is live, the whole question is how the newcomer
 * enters the rotation — see [Player.queueCredit] and [LateJoinerMode]. In the default
 * `FAIR_FORWARD` mode they start level with the least-played active player, which gives them an
 * equal share of games from now on without letting them push anyone else off a court, and leaves
 * the points earned by the people who arrived on time untouched.
 */
fun addPlayer(
    state: SessionState,
    name: String,
    id: String = nextPlayerId(state),
): SessionState {
    val resolvedName = uniqueName(state, name)
    if (resolvedName.isEmpty()) return state

    val queueCredit = when {
        state.status == SessionStatus.SETUP -> 0
        state.config.lateJoinerMode == LateJoinerMode.CATCH_UP -> 0
        else -> state.activePlayers.minOfOrNull { it.gamesPlayed } ?: 0
    }

    val player = Player(
        id = id,
        name = resolvedName,
        isActive = true,
        joinedAtRound = if (state.status == SessionStatus.SETUP) 0 else state.currentRoundIndex,
        queueCredit = queueCredit,
    )

    return state.copy(players = state.players + player)
}

/**
 * Mark a player as having left.
 *
 * Never deletes them: their games, wins and points stay on the leaderboard, because they played
 * those games. They simply stop being picked for rounds.
 */
fun removePlayer(state: SessionState, playerId: String): RemovalOutcome {
    val player = state.player(playerId)
        ?: return RemovalOutcome(state, affectsCurrentRound = false, affectedMatchId = null)

    val stuckIn = state.currentRound?.matches
        ?.firstOrNull { it.status == MatchStatus.PENDING && it.contains(playerId) }

    val next = state.copy(
        players = state.players.map {
            if (it.id == player.id) it.copy(isActive = false) else it
        }
    )

    return RemovalOutcome(
        state = next,
        affectsCurrentRound = stuckIn != null,
        affectedMatchId = stuckIn?.id,
    )
}

/** Bring someone back — the "just nipped out for a call" case. */
fun reinstatePlayer(state: SessionState, playerId: String): SessionState {
    val player = state.player(playerId) ?: return state
    if (player.isActive) return state

    // Re-entering is a late join: level with the least-played active player, so returning doesn't
    // hand them a queue advantage over everyone who stayed on court.
    val queueCredit = if (state.status == SessionStatus.ACTIVE) {
        (state.activePlayers.minOfOrNull { it.gamesPlayed } ?: 0) - player.gamesPlayed
    } else {
        0
    }

    return state.copy(
        players = state.players.map {
            if (it.id == playerId) {
                it.copy(
                    isActive = true,
                    queueCredit = queueCredit.coerceAtLeast(0),
                    restStreak = 0,
                )
            } else {
                it
            }
        }
    )
}

/**
 * Swap a resting player into a pending match in place of someone who has to go.
 *
 * The match keeps its identity and its court; only the name on the shirt changes.
 */
fun substituteInPendingMatch(
    state: SessionState,
    outgoingPlayerId: String,
    incomingPlayerId: String,
): SessionState {
    val round = state.currentRound ?: return state
    val match = round.matches.firstOrNull {
        it.status == MatchStatus.PENDING && it.contains(outgoingPlayerId)
    } ?: return state

    val incoming = state.player(incomingPlayerId) ?: return state
    if (!incoming.isActive) return state
    // Nobody plays two courts at once.
    if (round.playingIds.contains(incomingPlayerId)) return state

    val updatedMatch = match.copy(
        teamA = match.teamA.replacing(outgoingPlayerId, incomingPlayerId),
        teamB = match.teamB.replacing(outgoingPlayerId, incomingPlayerId),
    )

    val updatedRound = round.copy(
        matches = round.matches.map { if (it.id == match.id) updatedMatch else it },
        sittingOut = round.sittingOut - incomingPlayerId + listOfNotNull(
            outgoingPlayerId.takeIf { state.player(it)?.isActive == true }
        ),
    )

    return state.copy(
        players = state.players.map {
            if (it.id == incomingPlayerId) it.copy(restStreak = 0) else it
        },
        rounds = state.rounds.map { if (it.index == updatedRound.index) updatedRound else it },
    )
}

/** Players who could step into a match right now: active, and not already on a court this round. */
fun availableSubstitutes(state: SessionState, excluding: Set<String> = emptySet()): List<Player> {
    val onCourt = state.currentRound?.playingIds.orEmpty().toSet()
    return state.activePlayers.filter { it.id !in onCourt && it.id !in excluding }
}
