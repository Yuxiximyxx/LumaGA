package com.bugenzhao.mnga.ui.screens.topiclist


import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.DropdownImpl
import top.yukonga.miuix.kmp.basic.ListPopupColumn
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.extra.SuperListPopup
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.model.NavigationIdentifier
import com.bugenzhao.mnga.ui.components.GlassButton
import com.bugenzhao.mnga.ui.components.MiuixDropdownMenu
import com.bugenzhao.mnga.ui.components.MiuixMenuHeader
import com.bugenzhao.mnga.ui.components.MiuixMenuItem
import com.bugenzhao.mnga.model.PagingDataSource
import com.bugenzhao.mnga.model.PlusFeature
import com.bugenzhao.mnga.model.PlusModel
import com.bugenzhao.mnga.model.ToastModel
import com.bugenzhao.mnga.protos.datamodel.Forum
import com.bugenzhao.mnga.protos.datamodel.ForumId
import com.bugenzhao.mnga.protos.datamodel.PostReplyAction
import com.bugenzhao.mnga.protos.datamodel.Topic
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.protos.service.HotTopicListRequest
import com.bugenzhao.mnga.protos.service.HotTopicListResponse
import com.bugenzhao.mnga.protos.service.TopicListRequest
import com.bugenzhao.mnga.protos.service.TopicListResponse
import com.bugenzhao.mnga.storage.TopicListOrder
import com.bugenzhao.mnga.ui.components.PagedList
import com.bugenzhao.mnga.ui.components.GlassPanelBackground
import com.bugenzhao.mnga.ui.root.GlassVariantScale
import com.bugenzhao.mnga.ui.root.ScreenBackdropProvider
import com.bugenzhao.mnga.ui.root.TabDockBottomGap
import com.bugenzhao.mnga.ui.root.TabDockHeight
import com.bugenzhao.mnga.ui.root.tabDockContentBottomPadding
import com.bugenzhao.mnga.ui.root.glassContentColor
import com.bugenzhao.mnga.ui.root.recordedPageBackground
import com.bugenzhao.mnga.ui.root.rememberGlassSettings
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.bugenzhao.mnga.ui.components.SwipeToFavorBox
import com.bugenzhao.mnga.ui.components.toggleTopicFavor
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.nav.RouteDestination
import com.bugenzhao.mnga.ui.nav.TopicListMode
import com.xah.container.component.base.sharedContainer
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import kotlinx.coroutines.launch

private const val IdleAutoRefreshMillis = 60L * 60 * 1000 // 1 hour

/**
 * The workhorse topic list, a port of `TopicListView` plus the
 * `HotTopicListView` and `RecommendedTopicListView` variants selected by
 * [mode]. Handles order switching (two independent page caches), block-word
 * and forum-shortcut filtering, pull-to-refresh with a 1-hour idle
 * auto-refresh, toolbar More menu and the bottom action bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListScreen(
    navigator: Navigator,
    forumId: ForumId,
    mode: TopicListMode = TopicListMode.NORMAL,
    dateRange: Int = 0,
    editor: com.bugenzhao.mnga.ui.editor.EditorController? = null,
    asTabRoot: Boolean = false,
) {
    // The FAB's backdrop doubles as the screen backdrop (v1.7.6): the
    // list content root records it for the screen's whole lifetime and
    // every consumer — the FAB beside the list, the popup menus in the
    // Scaffold's host — sits outside that recording. Hoisted here so
    // the provider can wrap the Scaffold (see ScreenBackdrop.kt).
    val fabBackdrop = rememberLayerBackdrop()
    ScreenBackdropProvider(fabBackdrop) {
        TopicListScreenContent(
            navigator, forumId, mode, dateRange, editor, fabBackdrop,
            asTabRoot = asTabRoot,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicListScreenContent(
    navigator: Navigator,
    forumId: ForumId,
    mode: TopicListMode = TopicListMode.NORMAL,
    dateRange: Int = 0,
    editor: com.bugenzhao.mnga.ui.editor.EditorController? = null,
    fabBackdrop: com.kyant.backdrop.backdrops.LayerBackdrop,
    asTabRoot: Boolean = false,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val mock = forumId.hasFid() && forumId.fid.startsWith("mnga_")

    // -- Order state, following the default-order preference (SS5).
    var order by remember(forumId) { mutableStateOf<TopicListOrder?>(null) }
    val defaultOrderRaw by App.prefs.defaultTopicListOrderRaw.flow.collectAsState()
    LaunchedEffect(forumId) {
        if (order == null) order = TopicListOrder.fromRaw(defaultOrderRaw)
    }
    LaunchedEffect(defaultOrderRaw) {
        val latest = TopicListOrder.fromRaw(defaultOrderRaw)
        if (latest != order) order = latest
    }
    val orderOrDefault = order ?: TopicListOrder.fromRaw(defaultOrderRaw)

    // -- Data sources: held by the entry-scoped ViewModel so the loaded data
    // survives being covered by a pushed screen (composition is disposed,
    // ViewModel is not) — no snapshot needed.
    val topicListVM: TopicListViewModel = viewModel(
        factory = TopicListViewModel.factory(forumId, mode),
    )

    var hotRange by remember(forumId) {
        mutableStateOf(
            HotTopicListRequest.DateRange.forNumber(dateRange)
                ?: HotTopicListRequest.DateRange.DAY
        )
    }
    val dataSource = when (mode) {
        TopicListMode.HOT -> topicListVM.hotDataSource(hotRange)
        TopicListMode.RECOMMENDED -> topicListVM.recommendedDataSource
        TopicListMode.NORMAL ->
            if (orderOrDefault == TopicListOrder.POST_DATE) topicListVM.dataSourcePostDate
            else topicListVM.dataSourceLastPost
    }
    val state by dataSource.state.collectAsState()

    // Load on first entry only: after a pop-back the ViewModel still holds the
    // data, so notLoaded is false and nothing is refetched.
    LaunchedEffect(dataSource) {
        if (dataSource.notLoaded) dataSource.initialLoad()
    }

    // -- Forum meta enrichment (SS5 updateForumMeta).
    var forum by remember(forumId) {
        mutableStateOf(Forum.newBuilder().setId(forumId).build())
    }
    var parentForumName by remember(forumId) { mutableStateOf<String?>(null) }
    val responseForum: Forum? = when (val latest = state.latestResponse) {
        is TopicListResponse -> latest.forum
        is HotTopicListResponse -> latest.forum
        else -> null
    }
    LaunchedEffect(responseForum) {
        val response = responseForum ?: return@LaunchedEffect
        if (forum.id == response.id) {
            if (forum != response) forum = response
        } else if (forum.name != response.name && response.name.isNotEmpty()) {
            parentForumName = response.name
        }
    }

    val subforums = (state.latestResponse as? TopicListResponse)?.subforumsList.orEmpty()
    val toppedTopicID =
        (responseForum?.toppedTopicId ?: forum.toppedTopicId).takeIf { it.isNotEmpty() }

    // -- Favorites of forums.
    val favoriteForums by App.favoriteForums.forumsFlow().collectAsState()
    val isForumFavorite = favoriteForums.any { it.id == forum.id }
    val authInfo by App.authStorage.authInfo.collectAsState()
    val signedIn = authInfo.token.isNotEmpty()

    // -- Local favorite-topic overrides (kept after the swipe action).
    val favoredOverrides = remember(forumId) { mutableStateMapOf<String, Boolean>() }

    // -- Toolbar-triggered refresh (SS0.6): refresh then success haptic. The
    // refresh also resets the scroll to the top (via scrollToTopSignal) so
    // the newest items are visible.
    var refreshScrollEpoch by remember { mutableIntStateOf(0) }
    fun triggerRefresh() {
        refreshScrollEpoch++
        scope.launch {
            dataSource.refreshAsync(sleepMillis = 0).join()
            Haptics.play(view, Haptics.NotificationType.SUCCESS)
        }
    }

    // -- Auto refresh after 1h idle (SS0.6 / SS22.9).
    var lastSeenAt by remember(forumId, mode) { mutableStateOf(System.currentTimeMillis()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, dataSource) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> lastSeenAt = System.currentTimeMillis()
                Lifecycle.Event.ON_RESUME -> {
                    val lastRefresh = dataSource.state.value.lastRefreshTime?.time ?: 0L
                    val reference = maxOf(lastSeenAt, lastRefresh)
                    if (reference > 0 &&
                        System.currentTimeMillis() - reference > IdleAutoRefreshMillis
                    ) {
                        scope.launch {
                            dataSource.refreshAsync(sleepMillis = 0).join()
                            ToastModel.showAuto(ToastModel.Message.AutoRefreshed)
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // -- New topic flow (Plus-gated, via the global editor controller).
    fun newTopic() {
        if (!PlusModel.checkPlus(PlusFeature.NEW_TOPIC)) return
        editor?.newTopic(forum)
    }

    var moreMenuExpanded by remember { mutableStateOf(false) }
    var orderMenuExpanded by remember { mutableStateOf(false) }
    var shareMenuExpanded by remember { mutableStateOf(false) }
    var rangeMenuExpanded by remember { mutableStateOf(false) }
    var contextMenuTopic by remember { mutableStateOf<Topic?>(null) }

    val showRefreshButton by App.prefs.topicListShowRefreshButton.flow.collectAsState()

    // The refresh FAB floats over the list as liquid glass (v1.7.1): the
    // list content is recorded into fabBackdrop and the button samples it.
    // (The backdrop itself is remembered by the entry point above, which
    // also publishes it as this screen's menu/toast sampling source.)
    val glass = rememberGlassSettings()

    val navID = NavigationIdentifier.ForumID(forum.id)

    val screenTitle = when (mode) {
        TopicListMode.HOT -> L.str(context, "Hot Topics")
        TopicListMode.RECOMMENDED -> L.str(context, "Recommended Topics")
        TopicListMode.NORMAL -> forum.name
    }
    val subtitle = when (mode) {
        TopicListMode.HOT, TopicListMode.RECOMMENDED -> forum.name
        TopicListMode.NORMAL -> parentForumName
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        floatingActionButton = {
            if (showRefreshButton && !dataSource.isInitialLoading) {
                GlassButton(
                    backdrop = fabBackdrop,
                    spec = glass.spec,
                    scheme = glass.scheme,
                    // As a tab root the floating dock owns the bottom
                    // center — lift the FAB clear of it (v1.8.1).
                    modifier = if (asTabRoot) {
                        Modifier.padding(bottom = TabDockHeight + TabDockBottomGap)
                    } else {
                        Modifier
                    },
                    onClick = {
                        if (!state.isLoading && !state.isRefreshing) triggerRefresh()
                    },
                    size = 56.dp,
                    shape = RoundedCornerShape(16.dp),
                    debugShowSample = glass.debugShowSample,
                    debugColor = Color.Cyan,
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = L.str(context, "Refresh"),
                            tint = glassContentColor(glass.scheme),
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Large-title header (v1.8.0 MIUIX layout, per the confirmed
            // mock): back chevron, the forum name as a big title (the
            // parent forum rides as its subtitle), round action buttons.
            Row(
                Modifier.fillMaxWidth()
                    .padding(
                        start = if (asTabRoot) 16.dp else 4.dp,
                        end = 16.dp, top = 4.dp, bottom = 4.dp,
                    ),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
            ) {
                // As the home tab root (v1.8.1 custom home forum) there is
                // nothing to pop to — the back chevron goes away and the
                // title takes its place at the content edge.
                if (!asTabRoot) {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        screenTitle.ifEmpty { forumId.idDisplay() },
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    if (!subtitle.isNullOrEmpty()) {
                        Text(
                            subtitle,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        )
                    }
                }
                if (mode == TopicListMode.HOT) {
                    Box {
                        TopicListRoundButton(onClick = { rangeMenuExpanded = true }) {
                            Icon(
                                Icons.Outlined.DateRange,
                                contentDescription = L.str(context, "Range"),
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        // Flat single-choice menu (text + check only):
                        // natively expressible with Miuix ListPopup.
                        SuperListPopup(
                            show = rangeMenuExpanded,
                            alignment = PopupPositionProvider.Align.End,
                            onDismissRequest = { rangeMenuExpanded = false },
                        ) {
                            ListPopupColumn {
                                // Same glass panel layer as
                                // MiuixDropdownMenu (v1.7.6): the
                                // library paints its opaque panel
                                // under the column content, so the
                                // glass layer rides as the first
                                // child of a wrapping Box.
                                Box {
                                    GlassPanelBackground(
                                        shape = RoundedCornerShape(16.dp),
                                        lensScale = GlassVariantScale.MEDIUM,
                                        debugColor = Color(0xFFFF9800),
                                    )
                                    Column {
                                        val ranges = listOf(
                                            HotTopicListRequest.DateRange.DAY to "Last 24 hours",
                                            HotTopicListRequest.DateRange.WEEK to "Last week",
                                            HotTopicListRequest.DateRange.MONTH to "Last month",
                                        )
                                        ranges.forEachIndexed { index, (range, label) ->
                                            key(index) {
                                                DropdownImpl(
                                                    text = L.str(context, label),
                                                    optionSize = ranges.size,
                                                    isSelected = range == hotRange,
                                                    index = index,
                                                    onSelectedIndexChange = {
                                                        hotRange = ranges[it].first
                                                        rangeMenuExpanded = false
                                                    },
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (!mock && signedIn && editor != null) {
                        IconButton(onClick = { newTopic() }) {
                            Icon(
                                Icons.Outlined.Edit,
                                contentDescription = L.str(context, "New Topic"),
                            )
                        }
                    }
                    Box {
                        TopicListRoundButton(onClick = { moreMenuExpanded = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = L.str(context, "More"),
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        TopicListMoreMenu(
                            expanded = moreMenuExpanded,
                            onDismiss = { moreMenuExpanded = false },
                            orderMenuExpanded = orderMenuExpanded,
                            onOrderMenuShow = { orderMenuExpanded = true },
                            onOrderMenuDismiss = { orderMenuExpanded = false },
                            shareMenuExpanded = shareMenuExpanded,
                            onShareMenuShow = { shareMenuExpanded = true },
                            onShareMenuDismiss = { shareMenuExpanded = false },
                            mock = mock,
                            debugName = forumId.idDisplay() +
                                " " + (responseForum?.name ?: ""),
                            orderOrDefault = orderOrDefault,
                            onOrderChange = { order = it },
                            onHotTopics = {
                                navigator.push(
                                    Route.TopicList(forumId = forumId, mode = TopicListMode.HOT)
                                )
                            },
                            onRecommended = {
                                navigator.push(
                                    Route.TopicList(
                                        forumId = forumId,
                                        mode = TopicListMode.RECOMMENDED,
                                    )
                                )
                            },
                            toppedTopicID = toppedTopicID,
                            onToppedTopic = { id ->
                                navigator.push(Route.TopicDetails(topicId = id))
                            },
                            hasSubforums = subforums.isNotEmpty(),
                            onSubforums = {
                                navigator.push(Route.SubforumList(forumId = forumId))
                            },
                            onTopicSearch = {
                                navigator.push(Route.TopicSearch(forumId = forumId))
                            },
                            onHistory = { navigator.push(Route.History) },
                            onRefresh = { triggerRefresh() },
                            signedIn = signedIn,
                            isFavorite = isForumFavorite,
                            onToggleFavorite = {
                                App.favoriteForums.toggle(forum) {
                                    Haptics.lightImpact(view)
                                }
                            },
                            shareTitle = forum.name,
                            mngaURL = navID.mngaURL,
                            webpageURL = navID.webpageURL,
                        )
                    }
                    Spacer(Modifier.size(10.dp))
                    TopicListRoundButton(
                        onClick = { navigator.push(Route.TopicSearch(forumId = forumId)) },
                    ) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = L.str(context, "Search Topics"),
                            modifier = Modifier.size(20.dp),
                            tint = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
            }

            // Filter chips (the mock's capsule row): the two sort orders
            // switch this list in place; 精华 / 热帖 are the sibling
            // mode destinations (from a mode screen, the sort chips pop
            // back to the normal list and the other mode replaces).
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                TopicFilterChip(
                    text = "最新回复",
                    selected = mode == TopicListMode.NORMAL &&
                        orderOrDefault == TopicListOrder.LAST_POST,
                    onClick = {
                        if (mode == TopicListMode.NORMAL) {
                            order = TopicListOrder.LAST_POST
                        } else {
                            navigator.pop()
                        }
                    },
                )
                TopicFilterChip(
                    text = "最新发布",
                    selected = mode == TopicListMode.NORMAL &&
                        orderOrDefault == TopicListOrder.POST_DATE,
                    onClick = {
                        if (mode == TopicListMode.NORMAL) {
                            order = TopicListOrder.POST_DATE
                        } else {
                            navigator.pop()
                        }
                    },
                )
                TopicFilterChip(
                    text = "精华",
                    selected = mode == TopicListMode.RECOMMENDED,
                    onClick = {
                        when (mode) {
                            TopicListMode.NORMAL -> navigator.push(
                                Route.TopicList(
                                    forumId = forumId,
                                    mode = TopicListMode.RECOMMENDED,
                                )
                            )
                            TopicListMode.HOT -> navigator.replace(
                                Route.TopicList(
                                    forumId = forumId,
                                    mode = TopicListMode.RECOMMENDED,
                                )
                            )
                            TopicListMode.RECOMMENDED -> Unit
                        }
                    },
                )
                TopicFilterChip(
                    text = "热帖",
                    selected = mode == TopicListMode.HOT,
                    onClick = {
                        when (mode) {
                            TopicListMode.NORMAL -> {
                                if (PlusModel.checkPlus(PlusFeature.HOT_TOPIC)) {
                                    navigator.push(
                                        Route.TopicList(
                                            forumId = forumId,
                                            mode = TopicListMode.HOT,
                                        )
                                    )
                                }
                            }
                            TopicListMode.RECOMMENDED -> {
                                if (PlusModel.checkPlus(PlusFeature.HOT_TOPIC)) {
                                    navigator.replace(
                                        Route.TopicList(
                                            forumId = forumId,
                                            mode = TopicListMode.HOT,
                                        )
                                    )
                                }
                            }
                            TopicListMode.HOT -> Unit
                        }
                    },
                )
            }

            Box(
                Modifier.fillMaxSize().layerBackdrop(fabBackdrop)
                    .recordedPageBackground(),
            ) {
            PagedList(
                dataSource = dataSource,
                key = { it.id },
                // The toolbar already shows the loading spinner while the
                // first page loads; skip the centered one.
                showInitialLoading = false,
                emptyPlaceholder = L.str(context, "No Results"),
                groupedCard = true,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 8.dp,
                    end = 16.dp,
                    // As a tab root the list must also clear the floating
                    // dock at the bottom (v1.8.1).
                    bottom = if (asTabRoot) {
                        maxOf(
                            tabDockContentBottomPadding(),
                            if (showRefreshButton) 72.dp else 8.dp,
                        )
                    } else if (showRefreshButton) {
                        72.dp
                    } else {
                        8.dp
                    },
                ),
                scrollToTopSignal = refreshScrollEpoch,
                itemContent = { _, topic ->
                    TopicListItem(
                        topic = topic,
                        useTopicPostDate = mode == TopicListMode.RECOMMENDED ||
                            (mode == TopicListMode.NORMAL && orderOrDefault == TopicListOrder.POST_DATE),
                        // Topics of this very forum name no parent of their
                        // own; the forum being browsed is the answer for them.
                        fallbackForumName = forum.name.takeIf { !mock },
                        favoredOverride = favoredOverrides[topic.id],
                        contextMenuTopic = contextMenuTopic,
                        onContextMenu = { contextMenuTopic = it },
                        onDismissMenu = { contextMenuTopic = null },
                        onClick = {
                            navigator.push(
                                Route.TopicDetails(
                                    topicId = topic.id,
                                    fav = topic.fav.takeIf { it.isNotEmpty() },
                                )
                            )
                        },
                        onToggleFavor = {
                            toggleTopicFavor(
                                scope = scope,
                                view = view,
                                topicId = topic.id,
                                currentFavored = favoredOverrides[topic.id] ?: topic.isFavored,
                                onResult = { favored -> favoredOverrides[topic.id] = favored },
                            )
                        },
                        onNavigateToForum = { id ->
                            navigator.push(Route.TopicList(forumId = id))
                        },
                    )
                },
            )
            }
        }
    }
}

/** Round raised button of the topic list's large-title header (mock). */
@Composable
private fun TopicListRoundButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        content()
    }
}

/** One capsule of the topic list's filter row (the mock's chip bar). */
@Composable
private fun TopicFilterChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (selected) {
                    MiuixTheme.colorScheme.primary
                } else {
                    MiuixTheme.colorScheme.surfaceContainer
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Text(
            text,
            style = MiuixTheme.textStyles.body2,
            fontWeight = FontWeight.Medium,
            color = if (selected) {
                MiuixTheme.colorScheme.onPrimary
            } else {
                MiuixTheme.colorScheme.onSurface
            },
        )
    }
}

private fun ForumId.idDisplay(): String =
    if (hasFid()) "#$fid" else "st#$stid"

/**
 * One topic entry: swipe-to-favorite wrapper around either a forum-shortcut
 * row or the standard topic row with its long-press context menu.
 */
@Composable
private fun TopicListItem(
    topic: Topic,
    useTopicPostDate: Boolean,
    fallbackForumName: String?,
    favoredOverride: Boolean?,
    contextMenuTopic: Topic?,
    onContextMenu: (Topic?) -> Unit,
    onDismissMenu: () -> Unit,
    onClick: () -> Unit,
    onToggleFavor: () -> Unit,
    onNavigateToForum: (ForumId) -> Unit,
) {
    val context = LocalContext.current
    var shareMenuExpanded by remember { mutableStateOf(false) }

    SwipeToFavorBox(onFavor = onToggleFavor) {
        Box {
            if (topic.hasShortcutForum()) {
                ForumShortcutRow(
                    topic = topic,
                    onClick = { onNavigateToForum(topic.shortcutForum.id) },
                    onLongClick = { onContextMenu(topic) },
                )
            } else {
                TopicRow(
                    topic = topic,
                    useTopicPostDate = useTopicPostDate,
                    dimmedSubject = true,
                    showIndicators = true,
                    isFavored = favoredOverride,
                    fallbackForumName = fallbackForumName,
                    flat = true,
                    onClick = onClick,
                    onLongClick = { onContextMenu(topic) },
                    // Morph this card into the topic-details page (SharedNav
                    // shared container; key must equal the destination's).
                    sharedContainerKey = "topic-details/${topic.id}",
                )
            }

            // Miuix menu (phase 3) replacing the Material DropdownMenu:
            // the Share submenu swaps the popup content in place (Miuix
            // has no cascading menus) and dismissal is staged - an
            // outside tap first closes the submenu, like dismissing the
            // topmost Material popup did.
            MiuixDropdownMenu(
                expanded = contextMenuTopic == topic,
                onDismissRequest = {
                    if (shareMenuExpanded) {
                        shareMenuExpanded = false
                    } else {
                        onDismissMenu()
                    }
                },
                alignment = PopupPositionProvider.Align.Start,
            ) {
                if (shareMenuExpanded) {
                    val navID = topicNavID(topic)
                    ShareLinksMenuItems(
                        shareTitle = topicSubjectFull(topic),
                        mngaURL = navID.mngaURL,
                        webpageURL = navID.webpageURL,
                        onDone = {
                            shareMenuExpanded = false
                            onDismissMenu()
                        },
                    )
                } else {
                    MiuixMenuItem(
                        text = L.str(context, "Goto Topic"),
                        icon = Icons.Outlined.ArrowForward,
                        onClick = {
                            onClick()
                            onDismissMenu()
                        },
                    )
                    val title = topicSubjectContent(topic).trim()
                    if (title.isNotEmpty()) {
                        MiuixMenuItem(
                            text = L.str(context, "Copy Title"),
                            icon = Icons.Outlined.ContentCopy,
                            onClick = {
                                copyToClipboard(context, title)
                                onDismissMenu()
                            },
                        )
                    }
                    MiuixMenuItem(
                        text = L.str(context, "Share"),
                        icon = Icons.Outlined.Share,
                        onClick = { shareMenuExpanded = true },
                    )
                }
            }
        }
    }
}

/** The three share entries (LumaGA link / NGA link / open in browser). */
@Composable
private fun ShareLinksMenuItems(
    shareTitle: String,
    mngaURL: String?,
    webpageURL: String?,
    onDone: () -> Unit,
) {
    val context = LocalContext.current
    mngaURL?.let { url ->
        MiuixMenuItem(
            text = L.str(context, "LumaGA Link"),
            icon = Icons.Outlined.Share,
            onClick = {
                shareText(context, "$shareTitle $url".trim())
                onDone()
            },
        )
    }
    webpageURL?.let { url ->
        MiuixMenuItem(
            text = L.str(context, "NGA Link"),
            icon = Icons.Outlined.Language,
            onClick = {
                shareText(context, "$shareTitle $url".trim())
                onDone()
            },
        )
        MiuixMenuItem(
            text = L.str(context, "Open in Browser"),
            icon = Icons.Outlined.OpenInBrowser,
            onClick = {
                openInBrowser(context, url)
                onDone()
            },
        )
    }
}

/** Forum-shortcut topic row (SS2 topic-shortcut variant). */
@Composable
private fun ForumShortcutRow(
    topic: Topic,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    com.bugenzhao.mnga.ui.screens.forumlist.ForumRowCard(
        forum = topic.shortcutForum,
        // Morph the shortcut row into the forum's topic list (SharedNav
        // shared container; key = destination key).
        modifier = Modifier.sharedContainer(
            key = RouteDestination.keyFor(
                Route.TopicList(forumId = topic.shortcutForum.id)
            ),
            shape = RoundedCornerShape(16.dp),
            containerColor = null,
        ),
        isFavorite = false,
        asTopicShortcut = topic,
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

/** The toolbar More menu (SS5). */
@Composable
private fun TopicListMoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    orderMenuExpanded: Boolean,
    onOrderMenuShow: () -> Unit,
    onOrderMenuDismiss: () -> Unit,
    shareMenuExpanded: Boolean,
    onShareMenuShow: () -> Unit,
    onShareMenuDismiss: () -> Unit,
    mode: TopicListMode = TopicListMode.NORMAL,
    mock: Boolean,
    debugName: String,
    orderOrDefault: TopicListOrder,
    onOrderChange: (TopicListOrder) -> Unit,
    onHotTopics: () -> Unit,
    onRecommended: () -> Unit,
    toppedTopicID: String?,
    onToppedTopic: (String) -> Unit,
    hasSubforums: Boolean,
    onSubforums: () -> Unit,
    onTopicSearch: () -> Unit,
    onHistory: () -> Unit,
    onRefresh: () -> Unit,
    signedIn: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    shareTitle: String,
    mngaURL: String?,
    webpageURL: String?,
) {
    val context = LocalContext.current
    // Miuix menu (phase 3) replacing the Material DropdownMenu: a single
    // popup whose content swaps between the main items and the Order /
    // Share submenus (Miuix has no cascading menus); dismissal is staged
    // like the Material popup stack - an outside tap closes the submenu
    // first, then the menu itself. Item actions are unchanged.
    MiuixDropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            when {
                orderMenuExpanded -> onOrderMenuDismiss()
                shareMenuExpanded -> onShareMenuDismiss()
                else -> onDismiss()
            }
        },
    ) {
        when {
            orderMenuExpanded -> {
                MiuixMenuItem(
                    text = L.str(context, "Latest Replies"),
                    showCheck = orderOrDefault == TopicListOrder.LAST_POST,
                    onClick = { onOrderChange(TopicListOrder.LAST_POST) },
                )
                MiuixMenuItem(
                    text = L.str(context, "Latest Topics"),
                    showCheck = orderOrDefault == TopicListOrder.POST_DATE,
                    onClick = { onOrderChange(TopicListOrder.POST_DATE) },
                )
            }
            shareMenuExpanded -> {
                ShareLinksMenuItems(
                    shareTitle = shareTitle,
                    mngaURL = mngaURL,
                    webpageURL = webpageURL,
                    onDone = {
                        onShareMenuDismiss()
                        onDismiss()
                    },
                )
            }
            else -> {
                if (!mock) {
                    MiuixMenuHeader(debugName)
                    MiuixMenuItem(
                        text = L.str(context, "Order by"),
                        trailingText = L.str(
                            context,
                            if (orderOrDefault == TopicListOrder.POST_DATE) {
                                "Latest Topics"
                            } else {
                                "Latest Replies"
                            },
                        ),
                        onClick = onOrderMenuShow,
                    )
                    MiuixMenuItem(
                        text = L.str(context, "Hot Topics"),
                        icon = Icons.Outlined.LocalFireDepartment,
                        onClick = {
                            if (PlusModel.checkPlus(PlusFeature.HOT_TOPIC)) onHotTopics()
                            onDismiss()
                        },
                    )
                    MiuixMenuItem(
                        text = L.str(context, "Recommended Topics"),
                        icon = Icons.Outlined.ThumbUp,
                        onClick = {
                            onRecommended()
                            onDismiss()
                        },
                    )
                    if (toppedTopicID != null) {
                        MiuixMenuItem(
                            text = L.str(context, "Topped Topic"),
                            icon = Icons.Outlined.PushPin,
                            onClick = {
                                onToppedTopic(toppedTopicID)
                                onDismiss()
                            },
                        )
                    }
                }

                MiuixMenuItem(
                    text = L.str(context, "Share"),
                    icon = Icons.Outlined.Share,
                    onClick = onShareMenuShow,
                )

                if (hasSubforums) {
                    MiuixMenuItem(
                        text = L.str(context, "Subforums"),
                        icon = Icons.Outlined.Category,
                        onClick = {
                            onSubforums()
                            onDismiss()
                        },
                    )
                }
                if (!mock) {
                    MiuixMenuItem(
                        text = L.str(context, "Search Topics"),
                        icon = Icons.Outlined.Search,
                        onClick = {
                            onTopicSearch()
                            onDismiss()
                        },
                    )
                }
                MiuixMenuItem(
                    text = L.str(context, "History"),
                    icon = Icons.Filled.HistoryEdu,
                    onClick = {
                        if (PlusModel.checkPlus(PlusFeature.TOPIC_HISTORY)) onHistory()
                        onDismiss()
                    },
                )
                MiuixMenuItem(
                    text = L.str(context, "Refresh"),
                    icon = Icons.Outlined.Refresh,
                    onClick = {
                        onRefresh()
                        onDismiss()
                    },
                )
                MiuixMenuItem(
                    text = L.str(
                        context,
                        if (isFavorite) "Remove from Favorites" else "Mark as Favorite",
                    ),
                    icon = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    onClick = {
                        onToggleFavorite()
                        onDismiss()
                    },
                )
            }
        }
    }
}
