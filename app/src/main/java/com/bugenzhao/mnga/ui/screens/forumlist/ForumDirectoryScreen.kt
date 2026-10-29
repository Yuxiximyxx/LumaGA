package com.bugenzhao.mnga.ui.screens.forumlist

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlaylistAddCheck
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.Visibility
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.protos.datamodel.Forum
import com.bugenzhao.mnga.ui.components.LoadingRow
import com.bugenzhao.mnga.ui.components.MiuixDropdownMenu
import com.bugenzhao.mnga.ui.components.MiuixMenuItem
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.root.LocalDockBackdrop
import com.bugenzhao.mnga.ui.root.ScreenBackdropProvider
import com.bugenzhao.mnga.ui.root.rememberDockBackdropSourceModifier
import com.bugenzhao.mnga.ui.root.tabDockContentBottomPadding
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * The 版面 tab (v1.8.0): the forum directory in the confirmed MIUIX
 * layout — a large title (its search button is the page's only search
 * entry since v1.8.3; the duplicate search capsule is gone), the
 * user's pinned 「常逛版面」 as the first grouped card, then one
 * grouped card per forum category.
 *
 * Relation to the 主页 tab ([ForumListScreen]): home keeps its dense
 * favorites grid untouched; this screen is the browse-and-organize
 * face of the same data (it shares [ForumListViewModel]'s request via
 * its own entry-scoped instance). The pinned list is its own local
 * store ([com.bugenzhao.mnga.storage.PinnedForumsStorage]) — pinning
 * is not favoriting. Management: the 管理 toggle puts the pinned card
 * into edit mode (remove badges on pinned rows, add buttons on
 * category rows), and every row's long-press menu offers pin/unpin
 * plus the favorite toggle.
 *
 * Like the other tab roots, the screen records its own scrolling
 * content root into the dock backdrop and publishes it as the screen
 * backdrop for its popup menus (see DockBackdrop.kt / ScreenBackdrop.kt).
 */
@Composable
fun ForumDirectoryScreen(navigator: Navigator) {
    ScreenBackdropProvider(LocalDockBackdrop.current) {
        ForumDirectoryScreenContent(navigator)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ForumDirectoryScreenContent(navigator: Navigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val forumListVM: ForumListViewModel = viewModel()
    val dataSource = forumListVM.dataSource
    val state by dataSource.state.collectAsState()
    val categories = state.items

    val pinnedForums by App.pinnedForums.forums.collectAsState()
    val favoriteForums by App.favoriteForums.forumsFlow().collectAsState()

    LaunchedEffect(dataSource) {
        if (dataSource.notLoaded) {
            dataSource.initialLoad()
            App.favoriteForums.initialSync()
        }
        // First run only: seed 常逛 from the current favorites so the
        // section starts useful; afterwards the list is independent.
        if (!App.pinnedForums.isInitialized) {
            App.pinnedForums.seed(App.favoriteForums.favoriteForums)
        }
    }

    var editMode by remember { mutableStateOf(false) }
    var contextMenuForum by remember { mutableStateOf<Forum?>(null) }
    // v1.8.2: section visibility switches, from the title row's 显示 menu.
    var displayMenuExpanded by remember { mutableStateOf(false) }
    val showPinned by App.prefs.directoryShowPinned.flow.collectAsState()
    val showFavorites by App.prefs.directoryShowFavorites.flow.collectAsState()

    val refresh: () -> Unit = {
        scope.launch {
            dataSource.refreshAsync(sleepMillis = 500)
            App.favoriteForums.sync()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
    ) { padding ->
        val navStack by navigator.stack.collectAsState()
        val dockSource = rememberDockBackdropSourceModifier(
            navStack.lastOrNull() is Route.Forums,
        )
        PullToRefresh(
            isRefreshing = state.isRefreshing,
            onRefresh = refresh,
            modifier = Modifier.fillMaxSize().padding(padding).then(dockSource),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = tabDockContentBottomPadding(),
                ),
            ) {
                // ---- Large title + actions.
                item(key = "directory-title") {
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "版面",
                            style = MiuixTheme.textStyles.title1,
                            fontWeight = FontWeight.Bold,
                            color = MiuixTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        RoundBarButton(
                            onClick = { editMode = !editMode },
                            contentDescription = "管理常逛版面",
                        ) {
                            Icon(
                                if (editMode) Icons.Outlined.Check
                                else Icons.Outlined.PlaylistAddCheck,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        // v1.8.2: 显示 menu — the two section switches.
                        // (The pinned section's own 管理 button was the
                        // duplicate of the manage toggle above and is
                        // gone; this button carries display options.)
                        Box {
                            RoundBarButton(
                                onClick = { displayMenuExpanded = true },
                                contentDescription = "显示设置",
                            ) {
                                Icon(
                                    Icons.Outlined.Visibility,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MiuixTheme.colorScheme.onSurface,
                                )
                            }
                            MiuixDropdownMenu(
                                expanded = displayMenuExpanded,
                                onDismissRequest = { displayMenuExpanded = false },
                                alignment = PopupPositionProvider.Align.End,
                            ) {
                                MiuixMenuItem(
                                    text = "显示常逛版面",
                                    showCheck = showPinned,
                                    onClick = {
                                        App.prefs.directoryShowPinned.value = !showPinned
                                    },
                                )
                                MiuixMenuItem(
                                    text = "显示收藏版面",
                                    showCheck = showFavorites,
                                    onClick = {
                                        App.prefs.directoryShowFavorites.value = !showFavorites
                                    },
                                )
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        // The page's only search entry (v1.8.3): the
                        // title-row button, the same pattern as the home
                        // and topic-list headers. The search capsule that
                        // used to sit under the title duplicated it and
                        // is gone.
                        RoundBarButton(
                            onClick = { navigator.push(Route.GlobalSearch()) },
                            contentDescription = L.str(context, "Search"),
                        ) {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                }

                // ---- Pinned （常逛版面）. Renders only while its 显示
                // switch is on (v1.8.2). Management lives solely in the
                // title row's toggle — the section header used to carry
                // a duplicate 管理 button, now removed.
                if (showPinned) {
                item(key = "pinned-header") {
                    DirectorySectionHeader(title = "常逛版面")
                }
                item(key = "pinned-card") {
                    if (pinnedForums.isEmpty()) {
                        Text(
                            "还没有常逛版面。点「管理」或长按下方任意版面即可添加。",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    } else {
                        DirectoryGroupCard {
                            pinnedForums.forEachIndexed { index, forum ->
                                if (index > 0) DirectoryDivider()
                                DirectoryForumRow(
                                    forum = forum,
                                    editMode = editMode,
                                    editAction = if (editMode) {
                                        DirectoryEditAction.Remove
                                    } else {
                                        null
                                    },
                                    onEditAction = { App.pinnedForums.remove(forum.id) },
                                    contextMenuForum = contextMenuForum,
                                    onContextMenu = { contextMenuForum = it },
                                    onDismissMenu = { contextMenuForum = null },
                                    navigator = navigator,
                                )
                            }
                        }
                    }
                }
                }

                // ---- Favorite forums, merged in from the old home
                // (v1.8.1): the directory is now the complete forum
                // overview — pinned, favorites and every category on
                // one page. Rendered as the classic home's 3-column
                // icon grid inside a group card; tap opens the forum,
                // long-press opens the pin/favorite/home menu.
                // Renders only while its 显示 switch is on (v1.8.2).
                if (showFavorites) {
                item(key = "favorites-header") {
                    DirectorySectionHeader(title = "收藏版面")
                }
                item(key = "favorites-card") {
                    val favorites = favoriteForums.distinctBy { forumIdKey(it.id) }
                    if (favorites.isEmpty()) {
                        Text(
                            "还没有收藏版面。长按下方任意版面即可收藏。",
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    } else {
                        DirectoryGroupCard {
                            Column(Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) {
                                favorites.chunked(3).forEach { rowForums ->
                                    Row(Modifier.fillMaxWidth()) {
                                        rowForums.forEach { forum ->
                                            DirectoryFavoriteCell(
                                                forum = forum,
                                                contextMenuForum = contextMenuForum,
                                                onContextMenu = { contextMenuForum = it },
                                                onDismissMenu = { contextMenuForum = null },
                                                navigator = navigator,
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                        repeat(3 - rowForums.size) {
                                            Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                }

                // ---- All forums per category.
                if (categories.isEmpty()) {
                    item(key = "directory-loading") { LoadingRow() }
                } else {
                    val visibleCategories = categories
                        .filterNot { it.id == "mnga" }
                        .distinctBy { it.id }
                    visibleCategories.forEach { category ->
                        item(key = "dir-cat-header-${category.id}") {
                            DirectorySectionHeader(title = category.name)
                        }
                        item(key = "dir-cat-card-${category.id}") {
                            val forums = category.forumsList.distinctBy { forumIdKey(it.id) }
                            DirectoryGroupCard {
                                forums.forEachIndexed { index, forum ->
                                    if (index > 0) DirectoryDivider()
                                    val pinned = App.pinnedForums.isPinned(forum.id)
                                    DirectoryForumRow(
                                        forum = forum,
                                        editMode = editMode,
                                        editAction = when {
                                            !editMode -> null
                                            pinned -> DirectoryEditAction.Added
                                            else -> DirectoryEditAction.Add
                                        },
                                        onEditAction = {
                                            if (!pinned) App.pinnedForums.add(forum)
                                        },
                                        contextMenuForum = contextMenuForum,
                                        onContextMenu = { contextMenuForum = it },
                                        onDismissMenu = { contextMenuForum = null },
                                        navigator = navigator,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Circular raised button of the directory's title row (per the mock). */
@Composable
private fun RoundBarButton(
    onClick: () -> Unit,
    contentDescription: String?,
    content: @Composable () -> Unit,
) {
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MiuixTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/** Section header above a grouped card; optional trailing action （管理）. */
@Composable
private fun DirectorySectionHeader(
    title: String,
    trailing: (@Composable () -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 4.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MiuixTheme.textStyles.footnote1,
            fontWeight = FontWeight.SemiBold,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.weight(1f),
        )
        trailing?.invoke()
    }
}

/** One grouped large-corner card (the mock's section container). */
@Composable
private fun DirectoryGroupCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MiuixTheme.colorScheme.surfaceContainer),
        content = content,
    )
}

/** Hairline divider between rows of a group card, inset past the icon. */
@Composable
private fun DirectoryDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 72.dp),
        thickness = 0.5.dp,
        color = MiuixTheme.colorScheme.dividerLine,
    )
}

private enum class DirectoryEditAction { Add, Added, Remove }

/**
 * One forum row of the directory: icon, name, info line and a chevron —
 * or, in edit mode, the add/remove affordance for the pinned list.
 * Long-press opens the pin/favorite context menu (suppressed in edit
 * mode, where the trailing button is the management path).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectoryForumRow(
    forum: Forum,
    editMode: Boolean,
    editAction: DirectoryEditAction?,
    onEditAction: () -> Unit,
    contextMenuForum: Forum?,
    onContextMenu: (Forum?) -> Unit,
    onDismissMenu: () -> Unit,
    navigator: Navigator,
) {
    val context = LocalContext.current
    val view = LocalView.current
    Box {
        Row(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        if (!editMode) {
                            navigator.push(Route.TopicList(forumId = forum.id))
                        }
                    },
                    onLongClick = if (editMode) null else ({ onContextMenu(forum) }),
                )
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ForumIcon(iconUrl = forum.iconUrl, name = forum.name, size = 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    forum.name,
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (forum.info.isNotEmpty()) {
                    Text(
                        forum.info,
                        style = MiuixTheme.textStyles.footnote2,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            when (editAction) {
                DirectoryEditAction.Remove -> IconButton(onClick = onEditAction) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "移出常逛",
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.error,
                    )
                }
                DirectoryEditAction.Add -> IconButton(onClick = onEditAction) {
                    Icon(
                        Icons.Outlined.Add,
                        contentDescription = "加入常逛",
                        modifier = Modifier.size(20.dp),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
                DirectoryEditAction.Added -> Icon(
                    Icons.Outlined.Check,
                    contentDescription = "已在常逛",
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                null -> Icon(
                    Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        val pinned = App.pinnedForums.isPinned(forum.id)
        val favorite = App.favoriteForums.isFavorite(forum.id)
        val homeForum by App.homeForum.forum.collectAsState()
        val isHome = homeForum?.let {
            forumIdKey(it.id) == forumIdKey(forum.id)
        } == true
        MiuixDropdownMenu(
            expanded = contextMenuForum == forum,
            onDismissRequest = onDismissMenu,
            alignment = PopupPositionProvider.Align.Start,
        ) {
            MiuixMenuItem(
                text = if (pinned) "移出常逛版面" else "加入常逛版面",
                icon = Icons.Outlined.PushPin,
                onClick = {
                    App.pinnedForums.toggle(forum)
                    onDismissMenu()
                },
            )
            MiuixMenuItem(
                text = L.str(
                    context,
                    if (favorite) "Remove from Favorites" else "Mark as Favorite",
                ),
                icon = if (favorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                onClick = {
                    App.favoriteForums.toggle(forum) { Haptics.lightImpact(view) }
                    onDismissMenu()
                },
            )
            MiuixMenuItem(
                text = if (isHome) "取消设为主页" else "设为主页",
                icon = Icons.Outlined.Home,
                onClick = {
                    App.homeForum.set(if (isHome) null else forum)
                    onDismissMenu()
                },
            )
        }
    }
}

/**
 * One cell of the favorites grid on the directory page (v1.8.1): the
 * classic home's icon-over-name tile, living inside the favorites
 * group card. Tap opens the forum; long-press opens the same
 * pin/favorite/home context menu as the list rows, trimmed to the
 * actions that make sense for an already-favorite forum.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DirectoryFavoriteCell(
    forum: Forum,
    contextMenuForum: Forum?,
    onContextMenu: (Forum?) -> Unit,
    onDismissMenu: () -> Unit,
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val view = LocalView.current
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { navigator.push(Route.TopicList(forumId = forum.id)) },
                    onLongClick = { onContextMenu(forum) },
                )
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ForumIcon(iconUrl = forum.iconUrl, name = forum.name, size = 36.dp)
            Spacer(Modifier.height(6.dp))
            Text(
                forum.name,
                style = MiuixTheme.textStyles.footnote2,
                color = MiuixTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        val pinned = App.pinnedForums.isPinned(forum.id)
        val homeForum by App.homeForum.forum.collectAsState()
        val isHome = homeForum?.let {
            forumIdKey(it.id) == forumIdKey(forum.id)
        } == true
        MiuixDropdownMenu(
            expanded = contextMenuForum == forum,
            onDismissRequest = onDismissMenu,
            alignment = PopupPositionProvider.Align.Start,
        ) {
            MiuixMenuItem(
                text = if (pinned) "移出常逛版面" else "加入常逛版面",
                icon = Icons.Outlined.PushPin,
                onClick = {
                    App.pinnedForums.toggle(forum)
                    onDismissMenu()
                },
            )
            MiuixMenuItem(
                text = L.str(context, "Remove from Favorites"),
                icon = Icons.Outlined.Star,
                onClick = {
                    App.favoriteForums.toggle(forum) { Haptics.lightImpact(view) }
                    onDismissMenu()
                },
            )
            MiuixMenuItem(
                text = if (isHome) "取消设为主页" else "设为主页",
                icon = Icons.Outlined.Home,
                onClick = {
                    App.homeForum.set(if (isHome) null else forum)
                    onDismissMenu()
                },
            )
        }
    }
}
