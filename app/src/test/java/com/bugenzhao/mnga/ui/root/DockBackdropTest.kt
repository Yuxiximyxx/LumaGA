package com.bugenzhao.mnga.ui.root

import com.bugenzhao.mnga.ui.nav.Route
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Contract of the dock backdrop recording routing (v1.7.2, extended to
 * four tab roots in v1.8.0): the tab-root screens record their own
 * scrolling content into the dock's backdrop; the shell wrapper records
 * in every other state, so exactly one node records at a time and the
 * dock never samples the whole keep-alive navigation host while it is
 * on screen.
 */
class DockBackdropTest {

    @Test
    fun `tab roots are their own backdrop screens`() {
        assertTrue(Route.ForumList.isDockBackdropScreen())
        assertTrue(Route.Forums.isDockBackdropScreen())
        assertTrue(Route.Favorites.isDockBackdropScreen())
        assertTrue(Route.PersonalCenter.isDockBackdropScreen())
    }

    @Test
    fun `other destinations and no destination are not backdrop screens`() {
        assertFalse(Route.Settings.isDockBackdropScreen())
        assertFalse(Route.History.isDockBackdropScreen())
        assertFalse((null as Route?).isDockBackdropScreen())
    }

    @Test
    fun `shell records exactly when a tab root does not`() {
        assertFalse(dockShellRecordsBackdrop(Route.ForumList))
        assertFalse(dockShellRecordsBackdrop(Route.Forums))
        assertFalse(dockShellRecordsBackdrop(Route.Favorites))
        assertFalse(dockShellRecordsBackdrop(Route.PersonalCenter))
        assertTrue(dockShellRecordsBackdrop(Route.Settings))
        assertTrue(dockShellRecordsBackdrop(null))
    }
}
