package com.bugenzhao.mnga.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.xah.navigation.model.dest.Destination

/**
 * Per-back-stack-entry owners, standing in for what a NavHost
 * `NavBackStackEntry` used to provide: a [ViewModelStore] scoped to the
 * entry (screens call `viewModel()` expecting one ViewModel per stack
 * entry, cleared when the entry leaves the stack), a [Lifecycle] that
 * is RESUMED only while the entry is on top and drops to CREATED while
 * covered (screens and the inline video player pause work off
 * `LocalLifecycleOwner` when covered), and — since v1.6.1 — the
 * saved-state contract a real back-stack entry also carries:
 * [SavedStateRegistryOwner] + [HasDefaultViewModelProviderFactory], so a
 * ViewModel whose constructor takes a `SavedStateHandle` can be created
 * under the entry at all. Without them, `viewModel()` falls back to a
 * no-arg-only factory with empty [CreationExtras], and creating such a
 * ViewModel throws `RuntimeException: Cannot create an instance of …`
 * during the screen's first composition. `FavoritesViewModel` is the
 * app's only SavedStateHandle ViewModel, which is why entering the
 * favorites screen (and only that screen) crashed on tap.
 *
 * The registry is restored with a null bundle: per-entry saved state is
 * kept in memory for the entry's lifetime (enough for the selected
 * favorites folder to survive pop-backs via the entry-scoped
 * ViewModel), but is not plumbed into the activity's registry, so it
 * does not survive process death the way it did under NavHost.
 *
 * Driven by [Navigator.syncOwners] from the SharedNav stack contents:
 * an instance that disappears from the stack is destroyed and its store
 * cleared, mirroring NavHost's entry teardown.
 */
class EntryOwner :
    ViewModelStoreOwner,
    LifecycleOwner,
    SavedStateRegistryOwner,
    HasDefaultViewModelProviderFactory {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SavedStateViewModelFactory()

    override val defaultViewModelCreationExtras: CreationExtras
        get() = MutableCreationExtras().apply {
            set(SAVED_STATE_REGISTRY_OWNER_KEY, this@EntryOwner)
            set(VIEW_MODEL_STORE_OWNER_KEY, this@EntryOwner)
        }

    private var destroyed = false

    init {
        // SavedStateRegistryController wiring, mirroring
        // ComponentActivity / NavBackStackEntry: attach first, restore
        // (fresh entries have nothing saved), then enable SavedStateHandle
        // support — all while the lifecycle is still INITIALIZED, as
        // enableSavedStateHandles() requires.
        savedStateRegistryController.performAttach()
        savedStateRegistryController.performRestore(null)
        enableSavedStateHandles()
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    /** RESUMED while this entry tops the stack, CREATED while covered. */
    fun setOnTop(onTop: Boolean) {
        if (destroyed) return
        if (onTop) {
            if (registry.currentState.isAtLeast(Lifecycle.State.CREATED) &&
                !registry.currentState.isAtLeast(Lifecycle.State.RESUMED)
            ) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
            }
        } else {
            if (registry.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            }
        }
    }

    fun destroy() {
        if (destroyed) return
        destroyed = true
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

/**
 * A SharedNav [Destination] carrying one app [Route]. Instances are
 * created per push (like NavHost created one back-stack entry per
 * `navigate()` call); equality is by route only, so SharedNav's
 * reuse-finding launch modes (`PopToExisting`, `Clear`) can locate the
 * existing entry for a route the way `popBackStack(route)` used to.
 */
class RouteDestination(
    val route: Route,
    val owner: EntryOwner = EntryOwner(),
) : Destination() {

    override val key: String = keyFor(route)

    override fun equals(other: Any?): Boolean =
        other is RouteDestination && other.route == route

    override fun hashCode(): Int = route.hashCode()

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current
        CompositionLocalProvider(
            LocalViewModelStoreOwner provides owner,
            LocalLifecycleOwner provides owner,
        ) {
            navigator.dispatch(route)
        }
    }

    companion object {
        /** Shared-container / registry key for a route. */
        fun keyFor(route: Route): String = when (route) {
            Route.ForumList -> "forum-list"
            Route.Forums -> "forums"
            is Route.TopicList -> "topic-list/${route.forumId.fidOrStid()}/${route.mode}/${route.dateRange}"
            is Route.TopicDetails -> "topic-details/${route.topicId}"
            is Route.UserProfile ->
                // Prefer the carried user object's id over the name so
                // pushes that only carry a `User` still get a per-user
                // key (they all used to collapse into "embedded", which
                // would alias every such profile to one shared-container
                // state). Falls back to the name, then the legacy bucket.
                "user-profile/${
                    route.userId
                        ?: route.user?.id?.takeIf { it.isNotEmpty() }
                        ?: route.userName
                        ?: "embedded"
                }"
            is Route.GlobalSearch -> "global-search"
            is Route.TopicSearch -> "topic-search/${route.forumId?.fidOrStid().orEmpty()}"
            Route.HotTopics -> "hot-topics"
            Route.Favorites -> "favorites"
            Route.History -> "history"
            Route.ShortMessages -> "short-messages"
            is Route.ShortMessageDetails -> "short-message-details/${route.id}"
            Route.Subforums -> "subforums"
            is Route.SubforumList -> "subforum-list/${route.forumId.fidOrStid()}"
            is Route.UnknownForum -> "unknown-forum/${route.name.orEmpty()}"
            Route.CacheSettings -> "cache-settings"
            Route.BlockWords -> "block-words"
            Route.BlockedUsers -> "blocked-users"
            Route.About -> "about"
            Route.Settings -> "settings"
            Route.Notifications -> "notifications"
            Route.ClockIn -> "clock-in"
            Route.PersonalCenter -> "personal-center"
        }

        private fun com.bugenzhao.mnga.protos.datamodel.ForumId.fidOrStid(): String = when {
            hasFid() -> fid
            hasStid() -> stid
            else -> ""
        }
    }
}

/**
 * Provides the [Navigator] to [RouteDestination.Content] (and anything
 * else composed inside the navigation host that needs it). Set once by
 * the root around the SharedNav host.
 */
val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("LocalNavigator not provided")
}
