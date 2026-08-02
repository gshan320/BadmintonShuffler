package com.example.badmintonshuffler.engine

/**
 * mulberry32 — a small, fast, seeded PRNG.
 *
 * Every random decision in the engine goes through this, so a given seed always produces the same
 * session. That is what lets the fairness tests simulate twenty rounds and assert on the outcome
 * instead of hoping. Kotlin's `Int` wraps at 32 bits exactly like the original JavaScript, so the
 * bit patterns match the reference implementation.
 *
 * Not cryptographically secure, and not meant to be — it decides who plays with whom.
 */
class SeededRng(seed: Int) {

    private var state: Int = seed

    /** Uniform in [0, 1). */
    fun nextFloat(): Float {
        state += 0x6D2B79F5
        var t = state
        t = (t xor (t ushr 15)) * (t or 1)
        t = t xor (t + (t xor (t ushr 7)) * (t or 61))
        return ((t xor (t ushr 14)) ushr 8).toFloat() / (1 shl 24).toFloat()
    }

    /** Uniform in [0, bound). */
    fun nextInt(bound: Int): Int {
        require(bound > 0) { "bound must be positive, was $bound" }
        return (nextFloat() * bound).toInt().coerceAtMost(bound - 1)
    }

    /** Fisher-Yates. Returns a new list; the input is untouched. */
    fun <T> shuffled(items: List<T>): List<T> {
        val out = items.toMutableList()
        for (i in out.lastIndex downTo 1) {
            val j = nextInt(i + 1)
            val tmp = out[i]
            out[i] = out[j]
            out[j] = tmp
        }
        return out
    }

    /** True with probability 1/n. Used for reservoir-style tie-breaking. */
    fun oneIn(n: Int): Boolean = n <= 1 || nextInt(n) == 0
}
