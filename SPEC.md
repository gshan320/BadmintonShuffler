# Court Shuffler — product spec

A badminton session manager for casual group play. The organiser sets up an afternoon, the app
decides who plays with whom on which court, and keeps it fair.

Nothing is saved anywhere. A session is one afternoon; closing the app ends it.

---

## 1. Session Setup

The organiser answers one question per screen, forward-only with a back button, progress dots on
every screen. Nothing else is on the screen — no side navigation, no tabs. Every screen writes to
the store immediately, so moving back and forward never loses input.

The order is fixed:

1. **Courts** — how many courts are available. Stepper, 1–8. Live hint: "2 courts = 8 players on at
   once."
2. **Time** — session start time and end time. End must be after start. Shows the session length.
3. **Pace** — average minutes per game. Stepper, 5–30, default 12. Shows the derived estimate:
   "About 14 rounds today."
4. **Scoring** — target score (15 / 21 / custom), points per win, points per loss. Sensible defaults
   are preselected so the screen can be passed with one tap.
5. **Players** — the final and most important setup screen. Text field + Add, a list of removable
   player chips, and a live count. Validation: no empty names; no duplicate names (suggest
   "Alex (2)"); minimum 4 players before continuing. Live readout: "12 players, 3 courts — 12 on
   court, 0 resting each round."

The players screen's primary button is **"Start shuffling"**. It writes the config, starts the
session, generates round 1, and goes to the live session screen.

Keyboard behaviour on the players screen is a requirement, not a polish item: the field stays above
the list and keeps focus after each add, so the organiser can type name-Enter-name-Enter for
fourteen people without touching the screen again.

## 2. Rotation Fairness

This is the core promise of the app.

- A **round** is every usable court filled with a doubles match (4 players). Players beyond capacity
  sit out that round and are first in line for the next one.
- **Equal games.** Across the session, no active player should have played meaningfully more games
  than any other. Target: the gap between the most-played and least-played active player never
  exceeds **1 game**.
- **No starvation.** Nobody sits out three rounds in a row.
- **Variety.** Avoid repeating the same partner or the same opponents until the engine has no better
  option. Repeating a partner is worse than repeating an opponent (weighted 3:1).

### How a round is generated

1. **Capacity.** `eligible` = active players. `playableSlots = min(courtCount * 4, floor(eligible/4) * 4)`.
   If that is 0, the round has no matches and everyone sits out.
2. **Selection queue.** Sort eligible players by `effectiveGames` ascending (where
   `effectiveGames = gamesPlayed + queueCredit`), then `restStreak` descending (longest wait goes
   first), then seeded random jitter. Take the first `playableSlots`. The remainder sit out and have
   their `restStreak` incremented.
3. **Team formation.** Never pair players naively in queue order. Generate 300 candidate
   arrangements by seeded-shuffling the selected players into groups of 4 and then into two teams of
   2, score each with

   ```
   cost = 3 * Σ partnerHistory[a][b] over every team
        + 1 * Σ opponentHistory counts over the 4 cross-pairs of every match
   ```

   and take the lowest cost. Ties broken by the seeded RNG.
4. **Courts.** Assign matches to courts 1..courtCount in order.

### The fairness indicator

The live screen always shows the current spread (most-played minus least-played, active players
only) as a pill: green at 0–1, amber at 2+. Tapping it opens a breakdown of who has played how
many games.

## 3. Scoring

- Scores are entered per game, e.g. 21–15. Equal scores and negative scores are rejected. A score
  that does not reach the target score is warned about but allowed.
- Both players on a winning team receive **identical win credit**. Individual totals then diverge
  naturally as players are shuffled into new teams.
- `sessionPoints = wins * pointsPerWin + losses * pointsPerLoss`.
- Recording a result also updates, for all four players: games played, rest streak (reset to 0),
  points for/against, win or loss, partner history for the teammate, and opponent history for both
  opponents.
- **Leaderboard sort**: session points ↓, point differential ↓, win rate ↓, points for ↓, games
  played ↑, name A–Z. Ranks handle ties properly — two players tied for 1st are both 1st and the
  next player is 3rd.

## 4. Mid-session Edits

Players can be added or removed at **any** time during a session.

### Late joiners — the rule that matters

Default mode is **`FAIR_FORWARD`**. A new player's `queueCredit` is set to the **minimum
`gamesPlayed` among currently active players**, so they enter the rotation queue level with the
least-played player. The consequences, which are the whole point:

- They get the same number of games as everyone else **from that moment on**.
- They never monopolise courts to "catch up", so they don't displace people who arrived on time.
- Their wins and points genuinely start at zero, so the early birds keep the lead they earned.

`queueCredit` is a rotation-queue device only. It is never shown to the user and never affects
stats, points, or the leaderboard.

The alternative `CATCH_UP` mode (`queueCredit = 0`, newcomer prioritised until level on games) is in
the config but is not the default.

The app explains this in its own words when a player is added: *"Sarah joins from round 5. She'll
get the same number of games as everyone from here on, but starts at 0 points."* That sentence
exists so nobody argues about it in the hall.

### Removal

- Removing a player sets `isActive = false`. Their record is **never deleted** — their stats stay on
  the leaderboard, shown dimmed under "Left early".
- If the removed player is in a pending match this round, the organiser is offered exactly two
  choices: **substitute a resting player** (with the next player in the rotation queue offered as
  the recommended pick, and any other resting player selectable), or **void this match** (nobody
  gets credit, the court frees up).
- Changes take effect from the **next** round unless a substitution is made into the current one.
  The UI says so, so the organiser is never surprised.
- Adding a player mid-round must not disturb any in-progress match.

## 5. Live Session

Top to bottom:

- Compact header: "Round 4", a subtle progress line ("about 9 rounds left, ends 6:00pm"), and the
  fairness pill.
- A vertical list of court cards, one per court, each showing court number, the four player names in
  their quadrants, and the card's state.
- A "Resting this round" strip. Visually quiet but present — people want to see they are up next.
- A bottom action bar reachable with a thumb: "Edit players" (secondary) and "Next round" (primary).
  "Next round" is disabled, with a line explaining why, until every court has a recorded score.
- An overflow menu containing "End session".

Score entry: tapping a pending court card opens a bottom sheet with two big score steppers, one per
team, labelled with that team's player names, plus quick-fill buttons for common results (21–x) so
entry is a two-tap job. Confirming writes the result. The card then flips to its completed state
showing the final score, the winning side clearly marked, and the points each player just earned,
animated once. Tapping a completed card allows correcting a mis-entered score before the round
advances.

## 6. End of Session

- A **live leaderboard**, reachable from the session header: full ranked list with points, W–L
  record, games played and point differential, updating as scores come in.
- **"End session"** asks for confirmation ("This locks the session. You can still see the
  results."), then freezes the session and shows the results screen.
- **Results** is the one place in this app where boldness is warranted:
  - The top three on a podium, first place visually dominant, each with name, earned points and W–L.
  - Ties handled honestly: two players tied for first are shown as two firsts, with the tiebreak
    that was applied stated in words.
  - A collapsible "Full standings" below the podium.
  - Session stats in small type: total games played, most games by one person, longest win streak,
    best partnership (the pair with the best combined record).
  - **"Share results"** copies a clean plain-text summary — top three, then full standings — to the
    clipboard, ready to paste into a WhatsApp group.
  - **"Clear session and start fresh"** at the bottom as a danger action. It requires confirmation,
    and the confirmation states plainly that nothing is saved anywhere and the results cannot be
    recovered. It wipes everything and returns to an empty home screen.
- The podium reveal animates once on mount, and respects the system reduce-motion setting.

## 7. Non-functional requirements

- **Read at arm's length.** This is a phone held in one sweaty hand in a noisy sports hall, glanced
  at between games. Legibility and big tap targets beat density. Minimum tap target 48dp.
- **The display stays awake** during an active session — the organiser's phone sits on a bench.
- **Accessibility**: content descriptions on all interactive elements, WCAG AA contrast,
  reduce-motion respected.
- **Small screens**: nothing clips or overflows at 375dp wide.
- **No accidental loss**: since nothing is persisted, navigating back out of an active session asks
  for confirmation, and a render crash offers "Continue session" rather than dropping state.
- **No double submission**: a fast double tap must not record a score twice.

## 8. Explicitly out of scope

Singles matches, court skill-tiering, handicaps, player photos, historical sessions across days,
accounts, sharing beyond clipboard text, and anything requiring a network.
