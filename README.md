# Court Shuffler

A badminton session manager for casual group play. You tell it how many courts you have, when you
are playing and who turned up; it decides who plays with whom on which court, keeps the games even,
and works out the standings.

Built for one organiser holding one phone in a noisy sports hall.

## What it does

- **Setup wizard** — courts, times, game length, scoring, players. One question per screen.
- **Fair rotation** — across a session, nobody is more than one game ahead of anyone else. Nobody
  sits out three rounds in a row. Partners and opponents keep changing while there are fresh
  combinations left.
- **Late joiners** — someone arriving at round 6 slots in level with the least-played player, so
  from that moment everyone gets an equal share of games. They don't monopolise courts to catch up,
  and because their points start at zero, whoever arrived on time keeps the lead they earned.
- **Mid-session edits** — add or remove anyone at any time. If someone has to leave mid-game you get
  exactly two choices: substitute a resting player, or void the match.
- **Scoring and standings** — per-game scores, a live leaderboard, and a results podium with a
  plain-text summary you can paste into a group chat.

## Nothing is saved, on purpose

There is no database, no file, no backend and no account. The whole session lives in memory and dies
when the app is closed — a session is one afternoon of badminton, not a record to keep.

Two consequences worth knowing:

- The app asks before you navigate out of a live session, and the display is kept awake while one is
  running, because the phone spends the afternoon on a bench.
- Rotating the phone or resizing for the keyboard is safe; the session is held in a ViewModel. Force
  quitting or swiping the app away is not.

If you ever decide you want session recovery, everything is in one `SessionState` object behind one
ViewModel — adding a persistence layer is a small, self-contained change. It is deliberately not
there today.

## Running it

`JAVA_HOME` needs to point at a real JDK. If yours is stale, Android Studio ships one:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

Then, with a phone plugged in and USB debugging on:

```sh
./gradlew :app:installDebug     # build and install on the connected device
./gradlew :app:testDebugUnitTest  # the engine test suite
./gradlew :app:lintDebug          # lint
```

Or just press Run in Android Studio.

### Getting it onto a phone for real

For a personal app used by one organiser, **a debug build installed over USB is the right answer**:
`./gradlew :app:installDebug` and it stays on the phone like any other app, with no dev server, no
account and no store listing. Re-run the command when you want to update it.

The alternatives, for completeness. A **release build** (`./gradlew :app:assembleRelease`) needs a
signing key you have to create and keep safe, and buys you a smaller, faster APK — worth it if you
start handing it to other people. **Play Store internal testing** adds account setup, a privacy
policy and a review wait, and is only worth it if you want over-the-air updates for a group. Neither
is justified for one phone.

### Requirements

- Android 8.0 (API 26) or newer
- Portrait only

## Layout of the code

```
app/src/main/java/com/example/badmintonshuffler/
  engine/     Pure Kotlin. Rotation, scoring, roster, fairness, seeded RNG.
              No Android imports, no clock, no unseeded randomness — all of it unit tested.
  model/      Types.kt: the whole data model, immutable.
  state/      SessionViewModel — a thin wrapper that delegates to the engine.
  ui/theme/   Tokens.kt: the palette, type scale and metrics. Screens use nothing else.
  ui/component/  Shared components, including the CourtCard.
  ui/screen/  Every screen.
app/src/test/  48 engine tests, including 20-round fairness simulations.
```

The important rule is that the fairness logic has no dependency on Android at all, so the promises
the app makes are testable on the JVM in milliseconds. See `CLAUDE.md` for the working agreement and
`SPEC.md` for what the app is supposed to do.

## Tests

```
./gradlew :app:testDebugUnitTest
```

48 tests, all deterministic via a seeded PRNG. They cover capacity, twenty-round fairness
simulations, starvation, partner variety, both late-joiner modes, mid-session removal and return,
scoring, leaderboard tie-breaking, engine purity, and the awkward edge cases (fewer than four
players, everyone leaving, the session running past its end time).

If one fails, fix the engine — the tests encode the product promise, not the implementation.
