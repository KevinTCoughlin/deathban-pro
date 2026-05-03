package dev.coughlin.deathban.testutils

import org.junit.jupiter.api.AfterEach
import kotlin.math.abs

/**
 * Base class for unit tests with common setup and teardown.
 * Provides automatic cleanup of mock resources and assertion helpers.
 */
open class BaseTestClass {
    protected val playerFactory = MockPlayerFactory

    @AfterEach
    fun tearDown() {
        playerFactory.clearAll()
    }

    /**
     * Assert that a float value is approximately equal to expected (within delta)
     */
    protected fun assertApproximate(
        expected: Float,
        actual: Float,
        delta: Float = 0.0001f,
    ) {
        if (abs(expected - actual) > delta) {
            throw AssertionError(
                "Expected $expected but got $actual (delta: $delta)",
            )
        }
    }

    /**
     * Assert that a double value is approximately equal to expected (within delta)
     */
    protected fun assertApproximate(
        expected: Double,
        actual: Double,
        delta: Double = 0.0001,
    ) {
        if (abs(expected - actual) > delta) {
            throw AssertionError(
                "Expected $expected but got $actual (delta: $delta)",
            )
        }
    }
}
