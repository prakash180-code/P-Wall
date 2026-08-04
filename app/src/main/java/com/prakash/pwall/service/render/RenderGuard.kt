package com.prakash.pwall.service.render

/**
 * Circuit breaker for render layers: if a layer throws repeatedly it is dropped
 * so one bad layer can never hot-loop the render thread (the wallpaper simply
 * loses that layer instead of crashing). Pure, JVM-testable.
 */
class RenderGuard(private val maxFailures: Int = 3) {

    private val failures = HashMap<String, Int>()

    /**
     * Records a failure for [id]. Returns true once the layer has failed
     * [maxFailures] times in a row and should be dropped.
     */
    fun onFailure(id: String): Boolean {
        val next = (failures[id] ?: 0) + 1
        failures[id] = next
        return next >= maxFailures
    }

    /** Clears the failure count after a successful draw. */
    fun reset(id: String) {
        failures.remove(id)
    }

    fun failuresOf(id: String): Int = failures[id] ?: 0

    fun resetAll() {
        failures.clear()
    }
}
