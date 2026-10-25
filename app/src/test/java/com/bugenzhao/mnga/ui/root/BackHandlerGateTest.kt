package com.bugenzhao.mnga.ui.root

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The root back handler's enable matrix (RootBackHandler.kt). The two
 * handlers must be mutually exclusive, and both must be off at the
 * root so Android 13+ keeps its system back-to-home behavior and
 * Android 12- keeps the double-tap-to-exit handler's exclusive claim.
 */
class BackHandlerGateTest {

    @Test
    fun `api 34 and above uses predictive when the stack can pop`() {
        assertTrue(predictiveBackEnabled(sdkInt = 34, canPop = true))
        assertTrue(predictiveBackEnabled(sdkInt = 36, canPop = true))
        assertFalse(discreteBackEnabled(sdkInt = 34, canPop = true))
    }

    @Test
    fun `api 33 and below uses discrete when the stack can pop`() {
        assertTrue(discreteBackEnabled(sdkInt = 33, canPop = true))
        assertTrue(discreteBackEnabled(sdkInt = 26, canPop = true))
        assertFalse(predictiveBackEnabled(sdkInt = 33, canPop = true))
    }

    @Test
    fun `at the root neither handler is enabled on any api level`() {
        for (sdk in listOf(26, 32, 33, 34, 36)) {
            assertFalse("sdk=$sdk", predictiveBackEnabled(sdkInt = sdk, canPop = false))
            assertFalse("sdk=$sdk", discreteBackEnabled(sdkInt = sdk, canPop = false))
        }
    }

    @Test
    fun `handlers are mutually exclusive across the matrix`() {
        for (sdk in 26..36) {
            for (canPop in listOf(false, true)) {
                assertFalse(
                    "sdk=$sdk canPop=$canPop",
                    predictiveBackEnabled(sdk, canPop) && discreteBackEnabled(sdk, canPop),
                )
            }
        }
    }
}
