package com.bugenzhao.mnga.ui.nav

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

/**
 * Regression test for the v1.6.0 favorites crash.
 *
 * [EntryOwner] stands in for a NavHost back-stack entry. Before v1.6.1
 * it implemented only `ViewModelStoreOwner` + `LifecycleOwner`, so
 * `viewModel()` inside a destination resolved the library's fallback
 * factory (public no-arg constructors only) with empty CreationExtras.
 * Creating a ViewModel whose constructor takes a [SavedStateHandle]
 * then threw `RuntimeException: Cannot create an instance of …` during
 * the screen's first composition. [FavoritesViewModel] is the app's
 * only such ViewModel, which is why entering Favorites — and only
 * Favorites — crashed deterministically on tap.
 *
 * These tests drive the exact creation path the UI uses (owner +
 * owner's default factory + owner's default extras) for both a
 * SavedStateHandle ViewModel and a plain one.
 */
class EntryOwnerViewModelCreationTest {

    class HandleViewModel(val savedStateHandle: SavedStateHandle) : ViewModel()

    class PlainViewModel : ViewModel()

    // LifecycleRegistry & friends demand a main thread; the rule makes
    // the test thread report as the main one.
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private fun providerFor(owner: EntryOwner): ViewModelProvider =
        ViewModelProvider.create(
            owner,
            owner.defaultViewModelProviderFactory,
            owner.defaultViewModelCreationExtras,
        )

    @Test
    fun `saved state handle view model can be created under an entry owner`() {
        val owner = EntryOwner()
        val viewModel = providerFor(owner)[HandleViewModel::class]
        // The handle is live storage, not a stub: values round-trip and a
        // second lookup returns the same ViewModel (and handle).
        viewModel.savedStateHandle["currentFolderId"] = "42"
        val again = providerFor(owner)[HandleViewModel::class]
        assertSame(viewModel, again)
        assertEquals("42", again.savedStateHandle["currentFolderId"])
    }

    @Test
    fun `plain view model can be created under an entry owner`() {
        val owner = EntryOwner()
        val viewModel = providerFor(owner)[PlainViewModel::class]
        assertSame(viewModel, providerFor(owner)[PlainViewModel::class])
    }

    @Test
    fun `destroying the entry clears its view models`() {
        val owner = EntryOwner()
        val first = providerFor(owner)[HandleViewModel::class]
        owner.destroy()
        val second = providerFor(owner)[HandleViewModel::class]
        assertNotSame(first, second)
    }
}
