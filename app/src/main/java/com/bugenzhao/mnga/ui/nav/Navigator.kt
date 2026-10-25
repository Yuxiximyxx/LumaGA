package com.bugenzhao.mnga.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import com.bugenzhao.mnga.ui.editor.EditorController
import com.bugenzhao.mnga.ui.root.RouteDispatcher
import com.xah.navigation.anim.effect.DefaultTransitionEffect
import com.xah.navigation.anim.effect.FadeTransitionEffect
import com.xah.navigation.controller.NavigationController
import com.xah.navigation.model.action.LaunchMode
import com.xah.navigation.model.anim.TransitionEffect
import com.xah.navigation.model.dest.Destination
import java.util.Collections
import java.util.IdentityHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A path-based navigation stack mirroring SwiftUI's `NavigationStack`
 * semantics, backed by SharedNav's [NavigationController] (phase 4;
 * previously backed by Navigation Compose's NavHostController).
 *
 * The public surface (push/pop/popToRoot/popTo/replace/contains/stack/size/
 * lastOp) is unchanged, so screens keep calling it exactly as before.
 * Internally every [Route] is pushed as a [RouteDestination]; the route
 * list exposed via [stack] is derived from the SharedNav back stack, so
 * programmatic navigation (deep links, context menus) and system back
 * presses stay in sync.
 *
 * System back is owned by the root-level `SharedNavBackHandler`
 * (ui/root/RootBackHandler.kt), which drives SharedNav's predictive
 * pop transition with the gesture on Android 14+ and restores the
 * screen when the gesture is cancelled. Screens must therefore not
 * intercept back with their own BackHandler just to call [pop]; doing
 * so wins over the root callback and silently disables the predictive
 * preview.
 *
 * Two NavHost behaviors are reproduced by the adapter because screens
 * rely on them:
 *  - entry-scoped ViewModels: each [RouteDestination] carries an
 *    [EntryOwner] (ViewModelStore + Lifecycle) that is provided as the
 *    composition locals while the destination is composed, RESUMED only
 *    while on top, and cleared once the entry leaves the stack;
 *  - covered entries stay composed: the root sets
 *    `NavigationController.enableKeepAlive = true`, so popping back
 *    resumes a screen instead of rebuilding and refetching it.
 */
class Navigator {

    /** How the top-most route most recently entered the stack; screens use it
     * to tell a fresh push from a pop-back (resume). */
    enum class Op { PUSH, POP }

    /** The SharedNav controller; attached by the root once remembered. */
    lateinit var controller: NavigationController
        internal set

    /** Start destination instance handed to `rememberNavController`. */
    val startDestination = RouteDestination(Route.ForumList)

    /**
     * Transition effects, configured by the root before the host composes
     * (their page effects want composition). Defaults are the library's
     * stock effects.
     */
    var pushEffect: TransitionEffect = DefaultTransitionEffect()
    var tabEffect: TransitionEffect = FadeTransitionEffect()

    /** Editor controller, provided by the root for [dispatch]. */
    var editor: EditorController? = null

    private val _stack = MutableStateFlow<List<Route>>(listOf(Route.ForumList))
    val stack: StateFlow<List<Route>> = _stack

    val current: Route? get() = _stack.value.lastOrNull()
    val size: Int get() = _stack.value.size

    /** PUSH after [push], POP after any pop-style operation. */
    var lastOp: Op = Op.PUSH
        private set

    /** Destination instances currently in the SharedNav stack (identity). */
    private val alive: MutableSet<RouteDestination> =
        Collections.newSetFromMap(IdentityHashMap())

    fun push(route: Route) {
        lastOp = Op.PUSH
        // reuse = false: NavHost's navigate() always created a fresh entry,
        // even when pushing a route equal to the current top.
        controller.push(
            RouteDestination(route),
            LaunchMode.Push(reuse = false),
            effectFor(route),
        )
    }

    fun pop() {
        lastOp = Op.POP
        controller.pop()
    }

    fun popToRoot() {
        if (size <= 1) return
        lastOp = Op.POP
        // Clear(reuse = true) locates the existing ForumList entry (route
        // equality) and drops everything above it — the SharedNav analogue
        // of popBackStack(forum-list, inclusive = false).
        controller.push(RouteDestination(Route.ForumList), LaunchMode.Clear(reuse = true))
    }

    fun popTo(index: Int) {
        val entry = controller.stack.getOrNull(index) ?: return
        lastOp = Op.POP
        controller.push(entry.destination, LaunchMode.PopToExisting(reuse = true))
    }

    fun replace(route: Route) {
        lastOp = Op.PUSH
        controller.push(
            RouteDestination(route),
            LaunchMode.Replace(reuse = false),
            effectFor(route),
        )
    }

    fun contains(predicate: (Route) -> Boolean): Boolean = _stack.value.any(predicate)

    private fun effectFor(route: Route): TransitionEffect =
        if (route is Route.PersonalCenter) tabEffect else pushEffect

    /** Renders [route]'s screen; called from [RouteDestination.Content]. */
    @Composable
    fun dispatch(route: Route) {
        RouteDispatcher(this, route, editor)
    }

    /**
     * Keeps [stack]/[lastOp] and the per-entry owners in sync with the
     * SharedNav back stack. Call once from composition:
     * `LaunchedEffect(navigator) { navigator.observe(this) }`.
     */
    fun observe(scope: CoroutineScope) {
        scope.launch {
            snapshotFlow { controller.stack.toList() }
                .collect { entries -> sync(entries.map { it.destination }) }
        }
    }

    private fun sync(destinations: List<Destination>) {
        val dests = destinations.filterIsInstance<RouteDestination>()
        val now: MutableSet<RouteDestination> =
            Collections.newSetFromMap(IdentityHashMap())
        now.addAll(dests)
        val removed = alive.filter { it !in now }
        alive.clear()
        alive.addAll(now)
        removed.forEach { it.owner.destroy() }
        dests.forEachIndexed { index, dest ->
            dest.owner.setOnTop(index == dests.lastIndex)
        }
        val routes = dests.map { it.route }
        val previous = _stack.value
        lastOp = when {
            routes.size > previous.size -> Op.PUSH
            routes.size < previous.size -> Op.POP
            else -> lastOp
        }
        _stack.value = routes
    }
}

/** One pushed screen. */
sealed class Route {
    /** Root forum list (also the implicit root of the main stack). */
    data object ForumList : Route()

    data class TopicList(
        val forumId: com.bugenzhao.mnga.protos.datamodel.ForumId,
        val categoryName: String? = null,
        /** Extra modes spawned from a topic list screen. */
        val mode: TopicListMode = TopicListMode.NORMAL,
        val dateRange: Int = 0,
    ) : Route()

    data class TopicDetails(
        val topicId: String,
        val fav: String? = null,
        val postId: String? = null,
        val authorId: String? = null,
        val anonymousAuthorOnly: Boolean = false,
        val localCache: Boolean = false,
        val startPage: Int? = null,
    ) : Route()

    data class UserProfile(
        val userId: String? = null,
        val userName: String? = null,
        val user: com.bugenzhao.mnga.protos.datamodel.User? = null,
    ) : Route()

    data class GlobalSearch(val query: String? = null) : Route()
    data class TopicSearch(
        val forumId: com.bugenzhao.mnga.protos.datamodel.ForumId? = null,
    ) : Route()
    data object HotTopics : Route()
    data object Favorites : Route()
    data object History : Route()
    data object ShortMessages : Route()
    data class ShortMessageDetails(val id: String) : Route()
    data object Subforums :
        Route()

    data class SubforumList(
        val forumId: com.bugenzhao.mnga.protos.datamodel.ForumId,
    ) : Route()

    data class UnknownForum(val name: String?) : Route()
    data object CacheSettings : Route()
    data object BlockWords : Route()
    data object BlockedUsers : Route()
    data object About : Route()
    data object Settings : Route()
    data object Notifications : Route()
    data object ClockIn : Route()
    data object PersonalCenter : Route()
}

enum class TopicListMode { NORMAL, HOT, RECOMMENDED }
