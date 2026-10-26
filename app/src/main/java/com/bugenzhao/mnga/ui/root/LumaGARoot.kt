package com.bugenzhao.mnga.ui.root

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.model.NavigationIdentifier
import com.bugenzhao.mnga.ui.nav.CardPageEffects
import com.bugenzhao.mnga.ui.nav.LocalNavigator
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.nav.TopicListMode
import com.bugenzhao.mnga.ui.screens.favorites.FavoritesScreen
import com.bugenzhao.mnga.ui.screens.forumlist.ForumListScreen
import com.bugenzhao.mnga.ui.screens.history.HistoryScreen
import com.bugenzhao.mnga.ui.screens.messages.ShortMessageDetailsScreen
import com.bugenzhao.mnga.ui.screens.messages.ShortMessageListScreen
import com.bugenzhao.mnga.ui.screens.misc.AboutScreen
import com.bugenzhao.mnga.ui.screens.misc.BlockWordsScreen
import com.bugenzhao.mnga.ui.screens.misc.CacheScreen
import com.bugenzhao.mnga.ui.screens.search.SearchScreen
import com.bugenzhao.mnga.ui.screens.subforums.SubforumListScreen
import com.bugenzhao.mnga.ui.screens.topicdetails.TopicDetailsScreen
import com.bugenzhao.mnga.ui.screens.topiclist.TopicListScreen
import com.bugenzhao.mnga.ui.screens.user.UserProfileScreen
import com.bugenzhao.mnga.ui.screens.user.BlockedUsersScreen
import com.bugenzhao.mnga.ui.theme.LumaGATheme
import com.bugenzhao.mnga.model.appScope
import com.xah.navigation.anim.effect.DefaultTransitionEffect
import com.xah.navigation.anim.effect.FadeTransitionEffect
import com.xah.navigation.anim.effect.rememberDefaultPageEffects
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.xah.navigation.component.SharedNavHost
import com.xah.navigation.component.rememberNavController
import kotlinx.coroutines.flow.filter
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** Root composable: theme, navigation stack and global overlays. */
@Composable
fun LumaGARoot(onNewIntent: (android.content.Intent) -> Unit) {
    val prefs = App.prefs
    val themeColor by prefs.themeColorRaw.flow.collectAsState()
    val colorScheme by prefs.colorSchemeRaw.flow.collectAsState()

    LumaGATheme(
        themeColor = com.bugenzhao.mnga.storage.ThemeColor.fromRaw(themeColor),
        colorSchemeMode = com.bugenzhao.mnga.storage.ColorSchemeMode.fromRaw(colorScheme),
    ) {
        val editor = remember { com.bugenzhao.mnga.ui.editor.EditorController(appScope) }
        val navigator = remember { Navigator() }
        val defaultPageEffects = rememberDefaultPageEffects()

        // Page transitions (SharedNav): the background page gets the
        // library's Default geometry — it scales to 0.875 while its
        // corner radius interpolates against the device screen corner,
        // blurring and dimming behind the foreground page. The foreground
        // page uses CardPageEffects instead of the stock Default
        // foreground (which scales 0↔1 around a top-center pivot and made
        // plain pops collapse into a small floating rectangle): it is
        // floored at 0.875 around the screen center and fades through the
        // range below that floor. Within the predictive-gesture band the
        // frame is identical to stock, so the predictive-back preview —
        // current page shrinking into a rounded card over the restored
        // previous page, following the finger, snapping back when
        // cancelled — is unchanged. (The old NavHost slide geometry is
        // intentionally not reproduced: in SharedNav one effect drives
        // both the transition and its predictive preview.)
        // The Mine tab keeps its crossfade (tab switches are not pushes in
        // spirit), via the per-push fade effect below; its back preview is
        // therefore a fade as well.
        val pushEffect = remember {
            DefaultTransitionEffect(
                pageEffect = CardPageEffects(defaultPageEffects),
                pushAnimation = tween(280),
                popAnimation = tween(280),
            )
        }
        val tabEffect = remember {
            FadeTransitionEffect(pushAnimation = tween(280), popAnimation = tween(280))
        }
        val navController = rememberNavController(
            startDestination = navigator.startDestination,
            sharedTransitionEffect = pushEffect,
        )
        remember(navController) {
            navigator.controller = navController
            navigator.pushEffect = pushEffect
            navigator.tabEffect = tabEffect
            navigator.editor = editor
            // NavHost kept every back-stack entry composed; keepAlive does
            // the same under SharedNav so covered screens resume in place.
            navController.enableKeepAlive = true
        }

        // Opaque theme background under the navigation stack: during the
        // push/pop transitions both pages are partially transparent, and
        // without a solid layer beneath them the (always light) window
        // background flashes white in dark mode.
        // The content fills the whole surface and the tab dock floats over
        // it (v1.7.0): tab-root screens add bottom content padding (see
        // tabDockContentBottomPadding) so their last item can scroll clear
        // of the dock.
        // Backdrop for the tab dock's glass (Kyant): only the navigation
        // content is recorded — overlays, the editor and the dock itself
        // stay out of the sample. Who records follows DockBackdrop.kt:
        // on the two tab roots the visible screen records its own
        // scrolling content directly; this shell wrapper only records
        // while another destination is on top (dock hidden), so the
        // sample never has to come through the whole keep-alive host
        // while the dock is actually on screen.
        val dockBackdrop = rememberLayerBackdrop()
        // Second backdrop for the other glass faces (menus, toasts —
        // v1.7.3, see GlassOverlayBackdrop.kt): the same host wrapper
        // records it too, but only while a consumer is composed, so the
        // full-host re-render Kyant recording costs is paid only for
        // the lifetime of a transient overlay.
        val contentBackdrop = rememberLayerBackdrop()
        val overlayState = rememberGlassOverlayState()
        val rootStack by navigator.stack.collectAsState()
        val shellRecordsDock = dockShellRecordsBackdrop(rootStack.lastOrNull())
        val shellRecordsContent = overlayState.activeConsumers > 0
        Surface(Modifier.fillMaxSize(), color = MiuixTheme.colorScheme.background) {
            CompositionLocalProvider(
                LocalNavigator provides navigator,
                LocalDockBackdrop provides dockBackdrop,
                LocalContentBackdrop provides contentBackdrop,
                LocalGlassOverlayState provides overlayState,
            ) {
                Box(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .then(
                                if (shellRecordsDock) {
                                    Modifier.layerBackdrop(dockBackdrop)
                                } else {
                                    Modifier
                                },
                            )
                            .then(
                                if (shellRecordsContent) {
                                    Modifier.layerBackdrop(contentBackdrop)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        NavigationHost(navigator, editor)
                    }
                    MainTabBar(navigator, dockBackdrop)
                }
                GlobalOverlays(navigator, editor)
            }
        }

        // Pasteboard deep-link handling: when the newest clipboard entry is a
        // navigable NGA/LumaGA link that has not been jumped to yet, navigate
        // there directly. Android 12+ only lets a *focused* app read the
        // clipboard, and on a cold start onResume fires before the window
        // focus arrives (the focus-change callback itself can still race the
        // system's focus check), so the resume check is deferred until the
        // focus has settled.
        val view = androidx.compose.ui.platform.LocalView.current
        DisposableEffect(view) {
            val listener = android.view.ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
                if (hasFocus) view.post { maybeNavigateToPasteboardLink(navigator) }
            }
            view.viewTreeObserver.addOnWindowFocusChangeListener(listener)
            onDispose {
                view.viewTreeObserver.removeOnWindowFocusChangeListener(listener)
            }
        }
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(lifecycleOwner) {
            lifecycleOwner.lifecycle.currentStateFlow
                .filter { it == androidx.lifecycle.Lifecycle.State.RESUMED }
                .collect {
                    App.schemes.refreshPasteboardStatus()
                    // Wait for the window focus (typically well under 350ms)
                    // before touching the clipboard, otherwise the read is
                    // denied on Android 12+.
                    kotlinx.coroutines.delay(350)
                    maybeNavigateToPasteboardLink(navigator)
                }
        }
    }
}

/**
 * Bottom tab bar of the main shell: two tabs, Home and Mine, presented as
 * the floating dock in [FloatingTabDock] (v1.7.0, replacing the full-width
 * Miuix NavigationBar of MIUIX phase 2). The Mine tab is the personal-center
 * page, a root destination instead of a drawer-style page pushed over Home.
 * The dock only shows while a tab root is on top, hovering over the content
 * just above the system navigation area; tab switches never grow the stack
 * (Mine is pushed over Home, Home pops back to it), so back behavior and
 * the predictive preview stay exactly the host semantics used everywhere
 * else.
 */
@Composable
private fun BoxScope.MainTabBar(
    navigator: Navigator,
    backdrop: LayerBackdrop,
) {
    val glass = rememberGlassSettings()
    val stack by navigator.stack.collectAsState()
    val current = stack.lastOrNull()
    val onTabRoot = current is Route.ForumList || current is Route.PersonalCenter
    // Dock (re)appearance (v1.7.3): until now this used the DEFAULT
    // AnimatedVisibility transitions — a full-height slide over the
    // default 300ms tween — while the page transition underneath runs
    // 280ms and the backdrop's recording node hands over between the
    // shell wrapper and the tab-root screen in the same frames. The
    // dock therefore travelled its whole height on a different clock
    // than the page it lands on, fully opaque from the first frame,
    // with its glass sample switching source mid-flight — the " pops
    // in stiffly" feel in the user's recording. Now: a short fade with
    // a third-height rise (250ms enter / 150ms exit, easeOut), so the
    // dock materializes over the landing page; the fade covers the
    // first frames of the recording handover (the keep-alive tab root
    // is already the current destination — and already recording
    // itself — during the page transition, so the sample is settled
    // well before the fade completes).
    AnimatedVisibility(
        visible = onTabRoot,
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .navigationBarsPadding()
            .padding(bottom = TabDockBottomGap),
        enter = fadeIn(tween(250, easing = EaseOut)) +
            slideInVertically(tween(250, easing = EaseOut)) { it / 3 },
        exit = fadeOut(tween(150, easing = EaseOut)) +
            slideOutVertically(tween(150, easing = EaseOut)) { it / 3 },
    ) {
        FloatingTabDock(
            homeSelected = current is Route.ForumList,
            onHomeClick = { if (current is Route.PersonalCenter) navigator.pop() },
            onMineClick = {
                if (current is Route.ForumList) navigator.push(Route.PersonalCenter)
            },
            glassMaterial = glass.material,
            spec = glass.spec,
            scheme = glass.scheme,
            backdrop = backdrop,
            debugShowSample = glass.debugShowSample,
        )
    }
}

@Composable
internal fun NavigationHost(
    navigator: Navigator,
    editor: com.bugenzhao.mnga.ui.editor.EditorController?,
) {
    // 首页（导航栈只有根）双击返回退出：3 秒内按两次退出，第一次提示。
    // Android 13+ must leave root back to the system for the back-to-home
    // preview, so double-back-to-exit only applies on older Android versions.
    // sheet/弹窗打开时它们自己的返回处理优先（后注册的 BackHandler 先触发）。
    val stack by navigator.stack.collectAsState()
    val context = LocalContext.current
    var backPressedAt by remember { mutableLongStateOf(0L) }
    BackHandler(
        enabled = android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU &&
            stack.size <= 1
    ) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now - backPressedAt < 3000) {
            (context as? android.app.Activity)?.finish()
        } else {
            backPressedAt = now
            android.widget.Toast.makeText(
                context,
                "再按一次退出LumaGA",
                android.widget.Toast.LENGTH_SHORT,
            ).show()
        }
    }

    // Derive the route stack from the SharedNav back stack (system back
    // presses included), keeping navigator.stack/size/lastOp in sync.
    LaunchedEffect(navigator) { navigator.observe(this) }

    // 实验室功能「启动自动签到」：任意页面打开/切换时补一次签到检查
    // （Rust 缓存判定当天已签则零网络开销）。配合回前台触发覆盖所有
    // 应用活跃时机。(Was a NavController destination-changed listener;
    // the derived route stack flow is its SharedNav equivalent.)
    LaunchedEffect(navigator) {
        navigator.stack.collect {
            if (App.prefs.clockInEnabled.value &&
                App.prefs.autoClockInOnLaunch.value
            ) {
                App.currentUser.scheduleClockIn()
            }
        }
    }

    // System back is owned by SharedNavBackHandler below, driving
    // SharedNav's predictive API; the host's internal back handler is
    // disabled (v1.5.1 — see RootBackHandler.kt for why), and
    // destinations must not intercept system back just to call
    // navigator.pop().
    SharedNavHost(
        navController = navigator.controller,
        modifier = Modifier.fillMaxSize(),
        backHandler = {},
    )
    SharedNavBackHandler(navigator.controller)
}

/** Maps a route to its screen. */
@Composable
fun RouteDispatcher(
    navigator: Navigator,
    route: Route,
    editor: com.bugenzhao.mnga.ui.editor.EditorController? = null,
) {
    when (route) {
        is Route.ForumList ->
            ForumListScreen(navigator)
        is Route.TopicList ->
            TopicListScreen(
                navigator,
                forumId = route.forumId,
                mode = TopicListMode.NORMAL.takeIf { route.mode == TopicListMode.NORMAL }
                    ?: route.mode,
                dateRange = route.dateRange,
                editor = editor,
            )
        is Route.TopicDetails ->
            TopicDetailsScreen(navigator, route, editor = editor)
        is Route.UserProfile ->
            UserProfileScreen(
                navigator,
                userId = route.userId,
                userName = route.userName,
                user = route.user,
            )
        is Route.GlobalSearch -> SearchScreen(navigator, initialQuery = route.query)
        is Route.TopicSearch -> SearchScreen(navigator, route.forumId)
        is Route.Favorites -> FavoritesScreen(navigator)
        is Route.History -> HistoryScreen(navigator)
        is Route.ShortMessages -> ShortMessageListScreen(navigator)
        is Route.ShortMessageDetails -> ShortMessageDetailsScreen(navigator, route.id)
        is Route.SubforumList -> SubforumListScreen(navigator, route.forumId)
        is Route.CacheSettings -> CacheScreen(navigator)
        is Route.BlockWords -> BlockWordsScreen(navigator)
        is Route.BlockedUsers -> BlockedUsersScreen(navigator)
        is Route.About -> AboutScreen(navigator)
        is Route.ClockIn -> com.bugenzhao.mnga.ui.screens.user.ClockInScreen(navigator)
        is Route.PersonalCenter ->
            com.bugenzhao.mnga.ui.screens.user.PersonalCenterScreen(navigator)
        is Route.Settings ->
            com.bugenzhao.mnga.ui.screens.prefs.PreferencesSheet(
                onDismiss = { navigator.pop() },
                navigator = navigator,
            )
        is Route.Notifications ->
            com.bugenzhao.mnga.ui.screens.notifications.NotificationListSheet(
                navigator = navigator,
                onDismiss = { navigator.pop() },
            )
        else -> RoutePlaceholderScreen(navigator, route)
    }
}

@Composable
private fun RoutePlaceholderScreen(navigator: Navigator, route: Route) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            text = route.javaClass.simpleName,
            style = MiuixTheme.textStyles.title4,
            color = MiuixTheme.colorScheme.onBackground,
        )
    }
}

/** Toast hosts, global sheets and the deep-link destination overlay. */
@Composable
fun GlobalOverlays(
    navigator: Navigator,
    editor: com.bugenzhao.mnga.ui.editor.EditorController? = null,
) {
    com.bugenzhao.mnga.ui.components.toast.ToastHost()
    DeepLinkDestination(navigator)
    InAppBrowserOverlay()
    GlobalSheets(navigator, editor)
}

/** Sheets presented from anywhere, mirroring `GlobalSheetsModifier`. */
@Composable
private fun GlobalSheets(
    navigator: Navigator,
    editor: com.bugenzhao.mnga.ui.editor.EditorController? = null,
) {
    val context = androidx.compose.ui.platform.LocalContext.current

    // Editor sheets, mirroring the global `GlobalSheetsModifier`.
    if (editor != null) {
        val showPostEditor by editor.postReply.showEditor.collectAsState()
        if (showPostEditor) {
            com.bugenzhao.mnga.ui.editor.PostEditorSheet(editor.postReply) {
                editor.postReply.editorDismissed()
            }
        }
        val showSmEditor by editor.shortMessage.showEditor.collectAsState()
        if (showSmEditor) {
            com.bugenzhao.mnga.ui.editor.ShortMessageEditorSheet(editor.shortMessage) {
                editor.shortMessage.editorDismissed()
            }
        }
    }

    val isSigning by App.authStorage.isSigning.collectAsState()
    if (isSigning) {
        com.bugenzhao.mnga.ui.screens.login.LoginSheet(onDismiss = {
            App.authStorage.setIsSigning(false)
        })
    }
}

/** Presents the current deep-link destination as a fresh stack entry. */
@Composable
private fun DeepLinkDestination(navigator: Navigator) {
    val navID by App.schemes.navID.collectAsState()
    val id = navID ?: return
    LaunchedEffect(id) {
        when (id) {
            is NavigationIdentifier.TopicID ->
                navigator.push(Route.TopicDetails(topicId = id.tid, fav = id.fav))
            is NavigationIdentifier.PostID ->
                navigator.push(Route.TopicDetails(topicId = "", postId = id.pid))
            is NavigationIdentifier.ForumID ->
                navigator.push(Route.TopicList(forumId = id.id))
            is NavigationIdentifier.UserID ->
                navigator.push(Route.UserProfile(userId = id.uid))
            is NavigationIdentifier.UserNameID ->
                navigator.push(Route.UserProfile(userName = id.name))
            is NavigationIdentifier.Search ->
                navigator.push(Route.GlobalSearch(query = id.query.takeIf { it.isNotEmpty() }))
        }
        App.schemes.dismiss()
    }
}

/** In-app browser presentation for external links. */
@Composable
private fun InAppBrowserOverlay() {
    val url by App.openURL.inAppURL.collectAsState()
    val current = url ?: return
    com.bugenzhao.mnga.ui.components.InAppBrowserSheet(uri = current) {
        App.openURL.dismissInApp()
    }
}

// -- Pasteboard deep-link auto-jump -------------------------------------------
//
// On resume the app checks the newest clipboard entry; if it is a navigable
// NGA/LumaGA link that has not been jumped to yet, it navigates there and
// records the link as "jumped" so the same link is never auto-jumped again.

private const val JumpedPasteboardLinksKey = "jumpedPasteboardLinks"

private fun jumpedPasteboardLinks(): Set<String> =
    App.sharedPreferences.getStringSet(JumpedPasteboardLinksKey, emptySet()) ?: emptySet()

private fun recordJumpedPasteboardLink(link: String) {
    val set = jumpedPasteboardLinks().toMutableSet()
    set.add(link)
    App.sharedPreferences.edit().putStringSet(JumpedPasteboardLinksKey, set).apply()
}

/**
 * Whether this deep-link destination is already displayed as [route].
 * Used to skip the pasteboard auto-jump when the clipboard link points at
 * the page the user is already viewing (e.g. the "LumaGA Link" just copied
 * from the current topic's menu) instead of pointlessly reopening it.
 */
private fun NavigationIdentifier.matchesRoute(route: Route): Boolean = when (this) {
    is NavigationIdentifier.TopicID -> route is Route.TopicDetails && route.topicId == tid
    is NavigationIdentifier.PostID -> route is Route.TopicDetails && route.postId == pid
    is NavigationIdentifier.ForumID -> route is Route.TopicList && route.forumId == id
    is NavigationIdentifier.UserID -> route is Route.UserProfile && route.userId == uid
    is NavigationIdentifier.UserNameID -> route is Route.UserProfile && route.userName == name
    is NavigationIdentifier.Search -> route is Route.GlobalSearch
}

private fun maybeNavigateToPasteboardLink(navigator: Navigator) {
    val link = App.schemes.pasteboardLink() ?: return
    if (link in jumpedPasteboardLinks()) return
    // Skip the auto-jump when the clipboard link resolves to the destination
    // already on screen. This happens right after copying the current page's
    // own "LumaGA Link": without the check the app would dismiss and
    // re-present the very topic the user is reading.
    val id = runCatching { android.net.Uri.parse(link) }.getOrNull()
        ?.let { NavigationIdentifier.parse(it) }
    val current = navigator.current
    if (id != null && current != null && id.matchesRoute(current)) return
    // Only record the link once a jump actually happened (an invalid
    // clipboard entry is reported by navigateToPasteboardURL and returns
    // false, leaving the link eligible for the next resume).
    if (App.schemes.navigateToPasteboardURL()) {
        recordJumpedPasteboardLink(link)
    }
}
