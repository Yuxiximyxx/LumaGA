package com.bugenzhao.mnga.ui.screens.forumlist

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudQueue
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.model.NavigationIdentifier
import com.bugenzhao.mnga.model.PlusFeature
import com.bugenzhao.mnga.model.PlusModel
import com.bugenzhao.mnga.protos.datamodel.Forum
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.ui.components.LoadingRow
import com.bugenzhao.mnga.ui.components.MiuixDropdownMenu
import com.bugenzhao.mnga.ui.components.MiuixMenuItem
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.root.LocalDockBackdrop
import com.bugenzhao.mnga.ui.root.ScreenBackdropProvider
import com.bugenzhao.mnga.ui.root.rememberDockBackdropSourceModifier
import com.bugenzhao.mnga.ui.root.tabDockContentBottomPadding
import com.bugenzhao.mnga.ui.nav.RouteDestination
import com.xah.container.component.base.sharedContainer
import com.bugenzhao.mnga.ui.screens.topiclist.copyToClipboard
import com.bugenzhao.mnga.ui.screens.topiclist.openInBrowser
import com.bugenzhao.mnga.ui.screens.topiclist.shareText
import com.bugenzhao.mnga.storage.FilterMode
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private const val CollapsedCategoriesKey = "collapsedCategories"
private const val FavoritesSectionID = "LumaGA-Favorites"

/** Lazy-grid item key of a favorite cell, also its drag-reorder identity. */
private fun favoriteGridKey(forum: Forum): String = "fav-${forumIdKey(forum.id)}"

/**
 * The root forum sidebar, a port of `ForumListView`: favorite forums on top,
 * collapsible per-category sections below, plus the root toolbar
 * (notifications, search, filter). The personal center lives in the
 * bottom-bar "Mine" tab since the MIUIX phase-2 shell.
 */
/**
 * Entry point: publishes the dock backdrop — recorded by this
 * screen's own grid below — as the screen backdrop, so this screen's
 * popup menus (⋮ and long-press) sample the same in-screen recording
 * the dock itself uses (v1.7.6; see ScreenBackdrop.kt for why the
 * provider must wrap the whole Scaffold).
 */
@Composable
fun ForumListScreen(navigator: Navigator) {
    ScreenBackdropProvider(LocalDockBackdrop.current) {
        ForumListScreenContent(navigator)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ForumListScreenContent(
    navigator: Navigator,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // -- Data: all forum categories, held by the entry-scoped ViewModel so it
    // survives being covered by a pushed screen (composition is disposed,
    // ViewModel is not) — no snapshot needed.
    val forumListVM: ForumListViewModel = viewModel()
    val dataSource = forumListVM.dataSource
    val state by dataSource.state.collectAsState()
    val categories = state.items

    // Load (and sync favorites) on first entry only: after a pop-back the
    // ViewModel still holds the data, so notLoaded is false and nothing is
    // refetched.
    LaunchedEffect(dataSource) {
        if (dataSource.notLoaded) {
            dataSource.initialLoad()
            App.favoriteForums.initialSync()
        }
    }

    // -- Favorites & filter mode.
    val favoriteForums by App.favoriteForums.forumsFlow().collectAsState()
    val showAllRaw by App.favoriteForums.showAll.flow.collectAsState()
    val filterMode = FilterMode.fromRaw(showAllRaw)
    val useRemote by App.favoriteForums.useRemote.flow.collectAsState()
    val synced by App.favoriteForums.synced.collectAsState()

    // -- Collapsed category ids, persisted as a string set.
    var collapsedCategories by remember {
        mutableStateOf(
            App.sharedPreferences.getStringSet(CollapsedCategoriesKey, emptySet())?.toSet()
                ?: emptySet()
        )
    }

    fun toggleCollapsed(id: String) {
        collapsedCategories =
            if (id in collapsedCategories) collapsedCategories - id else collapsedCategories + id
        App.sharedPreferences.edit()
            .putStringSet(CollapsedCategoriesKey, collapsedCategories)
            .apply()
    }

    fun toggleFavorite(forum: Forum) {
        App.favoriteForums.toggle(forum) { Haptics.lightImpact(view) }
    }

    // -- Toolbar state.
    val unreadCount by App.notis.unreadCountAnimated.collectAsState()
    var editMode by remember { mutableStateOf(false) }
    // v1.8.2: the home-forum picker's primary entry is the top-left
    // button of this title bar (Settings keeps its own entry).
    var showHomeForumPicker by remember { mutableStateOf(false) }
    var moreMenuExpanded by remember { mutableStateOf(false) }
    var filterSubmenuExpanded by remember { mutableStateOf(false) }
    var contextMenuForum by remember { mutableStateOf<Forum?>(null) }

    // -- Drag-reorder state for the favorites grid (edit mode). Cells are
    // addressed by their lazy-grid key so the lifted cell always stays the one
    // the finger picked up, however often the list is reordered under it.
    val gridState = rememberLazyGridState()
    val favoriteKeys = remember(favoriteForums) { favoriteForums.map { favoriteGridKey(it) } }
    val reorder = rememberGridReorderState(
        gridState = gridState,
        keys = favoriteKeys,
        onMove = { from, to -> App.favoriteForums.move(from, to) },
        onSwap = { Haptics.lightImpact(view) },
    )
    // Leaving edit mode removes the drag modifiers, which can swallow the
    // gesture's own end event; drop the lift explicitly.
    LaunchedEffect(editMode) { if (!editMode) reorder.onDragEnd() }

    val refresh: () -> Unit = {
        scope.launch {
            dataSource.refreshAsync(sleepMillis = 500)
            App.favoriteForums.sync()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            // Transparent since v1.8.5 (no solid top band) — plain
            // transparent, no sampling layer under the bar and plain
            // icon buttons: the v1.8.4 glass circles here and
            // v1.8.5's full-bar glass background both sampled this
            // screen's backdrop from the bar slot, above the recorded
            // content's top edge — the same wrong-region sampling
            // that put magnified glyph fragments into the topic
            // list's header buttons, and the crash class that hit
            // Favorites / Personal Center in v1.8.5. Reverted to
            // the v1.8.3 plain form in v1.8.6 (see ScreenBackdrop.kt).
            TopAppBar(
                title = "泥坛",
                color = Color.Transparent,
                navigationIcon = {
                    // v1.8.2: home-forum customization lives at the home
                    // tab's top-left now (was Settings-only before).
                    if (!editMode) {
                        IconButton(onClick = { showHomeForumPicker = true }) {
                            Icon(
                                Icons.Outlined.Tune,
                                contentDescription = L.str(context, "Home Forum"),
                            )
                        }
                    }
                },
                actions = {
                    if (editMode) {
                        // Edit-favorites mode: hide every other action and show
                        // only "Done" on the far right.
                        TextButton(text = "完成", onClick = { editMode = false })
                        return@TopAppBar
                    }
                    val showBell = unreadCount > 0
                    if (showBell) {
                        IconButton(onClick = { navigator.push(Route.Notifications) }) {
                            BadgedBox(badge = { Badge { Text(unreadCount.toString()) } }) {
                                Icon(
                                    Icons.Outlined.Notifications,
                                    contentDescription = L.str(context, "Notifications"),
                                )
                            }
                        }
                    }
                    IconButton(onClick = { navigator.push(Route.GlobalSearch()) }) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = L.str(context, "Search"),
                        )
                    }
                    // "More" menu on the far right, holding the forum filters.
                    Box {
                        IconButton(onClick = { moreMenuExpanded = true }) {
                            Icon(
                                Icons.Outlined.MoreVert,
                                contentDescription = L.str(context, "More"),
                            )
                        }
                        MoreMenu(
                            expanded = moreMenuExpanded,
                            onDismiss = { moreMenuExpanded = false },
                            submenuExpanded = filterSubmenuExpanded,
                            onSubmenuDismiss = { filterSubmenuExpanded = false },
                            onSubmenuShow = { filterSubmenuExpanded = true },
                            filterMode = filterMode,
                            onFilterModeChange = { mode ->
                                App.favoriteForums.showAll.value = mode.raw
                            },
                            onEditFavorites = { editMode = true },
                            onHistory = { navigator.push(Route.History) },
                            anyCollapsed = collapsedCategories.isNotEmpty(),
                            onCollapseAll = {
                                collapsedCategories = categories.map { it.id }.toSet()
                                App.sharedPreferences.edit()
                                    .putStringSet(CollapsedCategoriesKey, collapsedCategories)
                                    .apply()
                            },
                            onExpandAll = {
                                collapsedCategories = emptySet()
                                App.sharedPreferences.edit()
                                    .putStringSet(CollapsedCategoriesKey, collapsedCategories)
                                    .apply()
                            },
                        )
                    }
                },
            )
        },
    ) { padding ->
        // While this tab root is current, its own scrolling content is
        // the tab dock's glass sample source (see DockBackdrop.kt).
        val navStack by navigator.stack.collectAsState()
        val dockSource = rememberDockBackdropSourceModifier(
            navStack.lastOrNull() is Route.ForumList,
        )
        PullToRefresh(
            isRefreshing = state.isRefreshing,
            onRefresh = refresh,
            modifier = Modifier.fillMaxSize().padding(padding).then(dockSource),
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                // Bottom clears the floating tab dock (tab-root screen):
                // the grid scrolls underneath it, so pad the end.
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 8.dp,
                    bottom = tabDockContentBottomPadding(),
                ),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // ---- Favorites section.
                item(
                    key = "favorites-header",
                    span = { GridItemSpan(maxLineSpan) },
                ) {
                    SectionHeader(
                        title = L.str(context, "Favorites"),
                        expanded = FavoritesSectionID !in collapsedCategories,
                        onToggle = { toggleCollapsed(FavoritesSectionID) },
                        trailing = {
                            if (useRemote) {
                                Icon(
                                    if (synced) Icons.Outlined.CloudDone else Icons.Outlined.CloudQueue,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        },
                    )
                }
                if (FavoritesSectionID !in collapsedCategories) {
                    if (favoriteForums.isEmpty()) {
                        item(key = "favorites-empty", span = { GridItemSpan(maxLineSpan) }) {
                            FavoritesEmptyHint()
                        }
                    } else {
                        if (editMode && favoriteForums.size > 1) {
                            item(
                                key = "favorites-reorder-hint",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                Text(
                                    "长按拖动调整顺序",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                )
                            }
                        }
                        items(
                            favoriteForums.distinctBy { forumIdKey(it.id) },
                            key = { forum -> favoriteGridKey(forum) },
                        ) { forum ->
                            val key = favoriteGridKey(forum)
                            ForumGridCellWithMenu(
                                forum = forum,
                                isFavorite = true,
                                editMode = editMode,
                                // Morph into the topic list from this cell
                                // (not in edit mode: cells drag instead).
                                sharedContainerKey = if (editMode) {
                                    null
                                } else {
                                    RouteDestination.keyFor(
                                        Route.TopicList(forumId = forum.id)
                                    )
                                },
                                contextMenuForum = contextMenuForum,
                                onContextMenu = { contextMenuForum = it },
                                onDismissMenu = { contextMenuForum = null },
                                onClick = { navigator.push(Route.TopicList(forumId = forum.id)) },
                                onToggleFavorite = { toggleFavorite(forum) },
                                onRemove = { toggleFavorite(forum) },
                                modifier = Modifier
                                    // Other cells glide into their new slots
                                    // while the lifted one tracks the finger.
                                    .then(
                                        if (reorder.isActive(key)) {
                                            Modifier
                                        } else {
                                            Modifier.animateItem()
                                        }
                                    )
                                    .then(
                                        if (editMode) {
                                            Modifier.reorderableGridCell(reorder, key)
                                        } else {
                                            Modifier
                                        }
                                    ),
                            )
                        }
                    }
                }

                // ---- All forums, per collapsible category.
                if (filterMode == FilterMode.ALL) {
                    if (categories.isEmpty()) {
                        item(key = "categories-loading", span = { GridItemSpan(maxLineSpan) }) {
                            LoadingRow()
                        }
                    } else {
                        // The "mnga" meta category is the app's own board; it
                        // never belongs on the forum home. Dedupe by id: server
                        // payloads can repeat a category, which would collide
                        // the grid item keys.
                        val visibleCategories = categories
                            .filterNot { it.id == "mnga" }
                            .distinctBy { it.id }
                        visibleCategories.forEach { category ->
                            item(
                                key = "cat-header-${category.id}",
                                span = { GridItemSpan(maxLineSpan) },
                            ) {
                                SectionHeader(
                                    title = category.name,
                                    expanded = category.id !in collapsedCategories,
                                    onToggle = { toggleCollapsed(category.id) },
                                )
                            }
                            if (category.id !in collapsedCategories) {
                                items(
                                    category.forumsList.distinctBy { forumIdKey(it.id) },
                                    key = { forum ->
                                        "cat-${category.id}-${forumIdKey(forum.id)}"
                                    },
                                ) { forum ->
                                    ForumGridCellWithMenu(
                                        forum = forum,
                                        isFavorite = App.favoriteForums.isFavorite(forum.id),
                                        editMode = false,
                                        // The favorites cell above owns the
                                        // shared-container registration for a
                                        // favorited forum (one state per
                                        // key); only register here when the
                                        // forum has no twin cell.
                                        sharedContainerKey =
                                            if (App.favoriteForums.isFavorite(forum.id)) {
                                                null
                                            } else {
                                                RouteDestination.keyFor(
                                                    Route.TopicList(forumId = forum.id)
                                                )
                                            },
                                        contextMenuForum = contextMenuForum,
                                        onContextMenu = { contextMenuForum = it },
                                        onDismissMenu = { contextMenuForum = null },
                                        onClick = {
                                            navigator.push(Route.TopicList(forumId = forum.id))
                                        },
                                        onToggleFavorite = { toggleFavorite(forum) },
                                        onRemove = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showHomeForumPicker) {
        HomeForumPickerDialog(onDismiss = { showHomeForumPicker = false })
    }
}

/** Collapsible section header with a rotating disclosure chevron (SS19). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectionHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 90f else 0f,
        animationSpec = tween(300),
        label = "chevron",
    )
    Row(
        Modifier.fillMaxWidth().clickable { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            trailing?.invoke()
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(20.dp).rotate(rotation),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
    }
}

/** Empty favorites hint: "No Favorites" (SS1). */
@Composable
private fun FavoritesEmptyHint() {
    val context = LocalContext.current
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                Icons.Outlined.StarBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Text(
                L.str(context, "No Favorites"),
                style = MaterialTheme.typography.bodyMedium,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}

/**
 * A grid cell (icon + name) with a long-press context menu (favorite toggle,
 * copy link, share) and an edit-mode remove badge.
 */
@Composable
private fun ForumGridCellWithMenu(
    forum: Forum,
    isFavorite: Boolean,
    editMode: Boolean,
    contextMenuForum: Forum?,
    onContextMenu: (Forum?) -> Unit,
    onDismissMenu: () -> Unit,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * SharedNav shared-container key for the topic-list destination this
     * cell opens (null = no morph). The registry holds ONE state per key,
     * so a forum visible in two cells at once (favorites grid + its
     * category cell) must register from only one of them — callers pass
     * the key for the favorites cell always, and for a category cell
     * only when the forum is not favorited.
     */
    sharedContainerKey: String? = null,
) {
    val navID = NavigationIdentifier.ForumID(forum.id)
    Box(
        modifier = modifier.then(
            if (sharedContainerKey != null) {
                Modifier.sharedContainer(
                    key = sharedContainerKey,
                    shape = RoundedCornerShape(16.dp),
                    containerColor = null,
                )
            } else {
                Modifier
            }
        )
    ) {
        ForumGridCell(
            forum = forum,
            editMode = editMode,
            onClick = onClick,
            // In edit mode the long-press is reserved for drag-reorder.
            onLongClick = if (editMode) null else ({ onContextMenu(forum) }),
            onRemove = onRemove,
        )
        val homeForum by App.homeForum.forum.collectAsState()
        val isHomeForum = homeForum?.let {
            forumIdKey(it.id) == forumIdKey(forum.id)
        } == true
        ForumContextMenu(
            expanded = contextMenuForum == forum,
            onDismiss = onDismissMenu,
            isFavorite = isFavorite,
            isHome = isHomeForum,
            navID = navID,
            onToggleFavorite = {
                onToggleFavorite()
                onDismissMenu()
            },
            onToggleHome = {
                App.homeForum.set(if (isHomeForum) null else forum)
                onDismissMenu()
            },
        )
    }
}

/** Dense 3-per-row grid cell: forum icon on top, single-line name below. */
@Composable
private fun ForumGridCell(
    forum: Forum,
    editMode: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    Box {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    enabled = true,
                    onClick = { if (!editMode) onClick() },
                    onLongClick = onLongClick,
                )
                .padding(horizontal = 4.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            ForumIcon(iconUrl = forum.iconUrl, name = forum.name, size = 36.dp)
            Text(
                forum.name,
                style = MaterialTheme.typography.labelMedium,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        if (editMode) {
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp),
            ) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = L.str(context, "Remove from Favorites"),
                    modifier = Modifier.size(14.dp),
                    tint = MiuixTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Shared long-press context menu for a forum (favorite / copy / share / open). */
@Composable
private fun ForumContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    isHome: Boolean,
    navID: NavigationIdentifier,
    onToggleFavorite: () -> Unit,
    onToggleHome: () -> Unit,
) {
    val context = LocalContext.current
    // Miuix menu (phase 3) replacing the Material DropdownMenu.
    MiuixDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        alignment = PopupPositionProvider.Align.Start,
    ) {
        MiuixMenuItem(
            text = L.str(
                context,
                if (isFavorite) "Remove from Favorites" else "Mark as Favorite",
            ),
            icon = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
            onClick = onToggleFavorite,
        )
        MiuixMenuItem(
            text = if (isHome) "取消设为主页" else "设为主页",
            icon = Icons.Outlined.Home,
            onClick = onToggleHome,
        )
        navID.mngaURL?.let { url ->
            MiuixMenuItem(
                text = L.str(context, "Copy Link"),
                icon = Icons.Outlined.ContentCopy,
                onClick = {
                    copyToClipboard(context, url)
                    onDismiss()
                },
            )
        }
        MiuixMenuItem(
            text = L.str(context, "Share"),
            icon = Icons.Outlined.Share,
            onClick = {
                val url = navID.webpageURL ?: navID.mngaURL
                if (url != null) shareText(context, url)
                onDismiss()
            },
        )
        navID.webpageURL?.let { url ->
            MiuixMenuItem(
                text = L.str(context, "Open in Browser"),
                icon = Icons.Outlined.OpenInBrowser,
                onClick = {
                    openInBrowser(context, url)
                    onDismiss()
                },
            )
        }
    }
}

/** The trailing "More" menu of the forum list, holding the forum filters. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    submenuExpanded: Boolean,
    onSubmenuShow: () -> Unit,
    onSubmenuDismiss: () -> Unit,
    filterMode: FilterMode,
    onFilterModeChange: (FilterMode) -> Unit,
    onEditFavorites: () -> Unit,
    onHistory: () -> Unit,
    anyCollapsed: Boolean,
    onCollapseAll: () -> Unit,
    onExpandAll: () -> Unit,
) {
    val context = LocalContext.current
    // Miuix menu (phase 3) replacing the Material DropdownMenu: the
    // Filters submenu swaps the popup content in place (Miuix has no
    // cascading menus) and dismissal is staged like the Material popup
    // stack. Item actions are unchanged.
    MiuixDropdownMenu(
        expanded = expanded,
        onDismissRequest = {
            if (submenuExpanded) onSubmenuDismiss() else onDismiss()
        },
    ) {
        if (submenuExpanded) {
            FilterMode.entries.forEach { mode ->
                MiuixMenuItem(
                    text = L.str(context, mode.raw),
                    icon = if (mode == FilterMode.FAVORITES_ONLY) {
                        Icons.Outlined.Star
                    } else {
                        Icons.Outlined.StarBorder
                    },
                    showCheck = mode == filterMode,
                    onClick = { onFilterModeChange(mode) },
                )
            }
        } else {
            MiuixMenuItem(
                text = L.str(context, "Edit Favorites"),
                icon = Icons.Outlined.PlaylistAddCheck,
                onClick = {
                    onEditFavorites()
                    onDismiss()
                },
            )
            MiuixMenuItem(
                text = L.str(context, "History"),
                icon = Icons.Filled.HistoryEdu,
                onClick = {
                    if (PlusModel.checkPlus(PlusFeature.TOPIC_HISTORY)) onHistory()
                    onDismiss()
                },
            )
            MiuixMenuItem(
                text = L.str(context, "Filters"),
                icon = Icons.Outlined.FilterList,
                onClick = onSubmenuShow,
            )
            if (filterMode == FilterMode.ALL) {
                if (!anyCollapsed) {
                    MiuixMenuItem(
                        text = L.str(context, "Collapse All Categories"),
                        icon = Icons.Outlined.ExpandLess,
                        onClick = {
                            onCollapseAll()
                            onDismiss()
                        },
                    )
                } else {
                    MiuixMenuItem(
                        text = L.str(context, "Expand All Categories"),
                        icon = Icons.Outlined.ExpandMore,
                        onClick = {
                            onExpandAll()
                            onDismiss()
                        },
                    )
                }
            }
        }
    }
}
