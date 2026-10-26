package com.bugenzhao.mnga.ui.root

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * Contract of the screen-backdrop registry (v1.7.6): participating
 * screens register their backdrop for their composition lifetime in
 * composition order; root-level consumers (the toast host) sample the
 * topmost one. Registration mirrors the navigation stack under
 * keep-alive: pushes register later, pops unregister on dispose.
 * (Tested with string tokens — the registry is generic; production
 * registers LayerBackdrop instances.)
 */
class ScreenBackdropRegistryTest {

    @Test
    fun `empty registry has no active backdrop`() {
        assertNull(ScreenBackdropRegistry<String>().activeBackdrop)
    }

    @Test
    fun `active is the most recently registered backdrop`() {
        val registry = ScreenBackdropRegistry<String>()
        registry.register("first")
        assertSame("first", registry.activeBackdrop)
        registry.register("second")
        assertSame("second", registry.activeBackdrop)
    }

    @Test
    fun `unregistering the top reveals the one below`() {
        val registry = ScreenBackdropRegistry<String>()
        registry.register("first")
        registry.register("second")
        registry.unregister("second")
        assertSame("first", registry.activeBackdrop)
        registry.unregister("first")
        assertNull(registry.activeBackdrop)
    }

    @Test
    fun `registering the same backdrop twice keeps one entry`() {
        val registry = ScreenBackdropRegistry<String>()
        registry.register("backdrop")
        registry.register("backdrop")
        registry.unregister("backdrop")
        assertNull(registry.activeBackdrop)
    }

    @Test
    fun `unregistering an unknown backdrop is a no-op`() {
        val registry = ScreenBackdropRegistry<String>()
        registry.register("backdrop")
        registry.unregister("unknown")
        assertSame("backdrop", registry.activeBackdrop)
    }
}
