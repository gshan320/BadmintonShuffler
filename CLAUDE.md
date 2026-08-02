# Court Shuffler — working agreement

A badminton session manager for casual group play. One organiser, one afternoon, one phone.

## Stack

- **Android native**: Kotlin + Jetpack Compose (Material 3), single-activity.
- **Navigation**: Navigation Compose, type-safe routes.
- **State**: a single `SessionViewModel` exposing `StateFlow<SessionState>`. No DI framework.
- **Tests**: JUnit 4 as local unit tests under `app/src/test/`.
- **minSdk 26** so `java.time` is available without desugaring.

> The original build plan (`app/src/main/keepRules/Prompts`) specified Expo + React Native +
> TypeScript + Zustand + Vitest. This repo is a Kotlin/Compose Android project, so the plan was
> translated 1:1 onto native Android. Every product requirement is unchanged; only the stack moved.
> Mapping: `src/engine/` → `engine/`, Zustand store → `SessionViewModel`, `src/theme/tokens.ts` →
> `ui/theme/Tokens.kt`, expo-router screens → `ui/screen/` + `NavGraph.kt`, Vitest → JUnit.

## Hard constraints

1. **NO persistence layer.** No Room, no DataStore, no SharedPreferences, no backend, no auth, no
   network calls. All session state lives in memory and dies with the process. This is intentional —
   a session is one afternoon of badminton. The one exception is Compose/Android-lifecycle state
   restoration *within* a live process (e.g. `rememberSaveable` for a text field); nothing is
   written to disk.
2. **The shuffle/rotation logic is pure Kotlin.** Everything under
   `com.example.badmintonshuffler.engine` has zero Compose, zero Android framework, and zero
   `android.*` imports, so it runs on the JVM in plain unit tests. Engine functions are pure: they
   take a state and return a new state, and never mutate their input.
3. **Determinism.** All randomness goes through `engine/Rng.kt` (seeded mulberry32). No
   `kotlin.random.Random.Default`, no `System.currentTimeMillis()` inside the engine — the caller
   passes time in.

## Code conventions

- Immutable data: `data class` with `val` only; collections are read-only (`List`, `Map`).
- Compose: stateless composables that take data + lambdas; hoist state to the ViewModel.
- No `!!`. Prefer `requireNotNull` / sealed results at boundaries.
- Types live in `model/Types.kt`.
- Every screen composable is `@Preview`-able without a ViewModel (take a state param, not a VM).
- **No magic numbers in screens** — sizes, spacing, colour and type come from `ui/theme/Tokens.kt`.
- Minimum tap target 48dp.

## Rules of engagement

- Never add a dependency without stating why in the commit message.
- Never add a feature that is not in `SPEC.md`.
- If a fairness test fails, fix the **engine**, not the test — the tests encode the product promise.

## Commands

`JAVA_HOME` in this environment may point at a stale Homebrew JDK. Prefix Gradle with a valid one:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :app:testDebugUnitTest   # engine tests
./gradlew :app:compileDebugKotlin  # typecheck
./gradlew :app:assembleDebug       # build APK
./gradlew :app:installDebug        # install onto a connected device
```
