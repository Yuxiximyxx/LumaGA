package com.bugenzhao.mnga.ui.screens.subforums

import androidx.compose.ui.graphics.Color


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.PopupPositionProvider
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.bugenzhao.mnga.ui.root.ScreenBackdropProvider
import com.bugenzhao.mnga.ui.root.rememberScreenBackdropSourceModifier
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.logicCallAsync
import com.bugenzhao.mnga.protos.datamodel.Forum
import com.bugenzhao.mnga.protos.datamodel.ForumId
import com.bugenzhao.mnga.protos.datamodel.Subforum
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.protos.service.SubforumFilterRequest
import com.bugenzhao.mnga.protos.service.SubforumFilterResponse
import com.bugenzhao.mnga.protos.service.TopicListResponse
import com.bugenzhao.mnga.ui.components.ListPlaceholder
import com.bugenzhao.mnga.ui.components.MiuixDropdownMenu
import com.bugenzhao.mnga.ui.components.MiuixMenuItem
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.nav.RouteDestination
import com.xah.container.component.base.sharedContainer
import com.bugenzhao.mnga.ui.screens.forumlist.forumIdKey
import com.bugenzhao.mnga.ui.screens.forumlist.ForumRow
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import kotlinx.coroutines.launch

private const val SubforumTipSeenKey = "subforumTipSeen"

/**
 * Subforum management, a port of `SubforumListView`: forum rows with a
 * subscribe/unsubscribe switch per subforum (subforumFilter request), tap to
 * navigate, long-press to favorite, and a first-visit tip card.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
/**
 * Entry point: publishes this screen's backdrop (v1.7.6) — recorded by
 * the content root below for the screen's whole lifetime, sampled by
 * its popup menu from the Scaffold's host (see ScreenBackdrop.kt).
 */
@Composable
fun SubforumListScreen(navigator: Navigator, forumId: ForumId) {
    val screenBackdrop = rememberLayerBackdrop()
    ScreenBackdropProvider(screenBackdrop) {
        SubforumListScreenContent(navigator, forumId)
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SubforumListScreenContent(navigator: Navigator, forumId: ForumId) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Entry-scoped ViewModel: the loaded subforum list survives pop-backs
    // (composition is disposed, ViewModel is not) — no refetch on return.
    val subforumListVM: SubforumListViewModel = viewModel(
        factory = SubforumListViewModel.factory(forumId),
    )
    val dataSource = subforumListVM.dataSource
    val state by dataSource.state.collectAsState()
    LaunchedEffect(dataSource) {
        if (dataSource.notLoaded) dataSource.initialLoad()
    }

    // Local editable copy, updated optimistically and re-synced on refresh.
    val subforums = remember { mutableStateListOf<Subforum>() }
    LaunchedEffect(state.items, state.lastRefreshTime) {
        subforums.clear()
        subforums.addAll(state.items)
    }

    val forumName = (state.latestResponse as? TopicListResponse)?.forum?.name
        ?.ifEmpty { null }
        ?: (if (forumId.hasFid()) "#${forumId.fid}" else "st#${forumId.stid}")
    val favoriteForums by App.favoriteForums.forumsFlow().collectAsState()

    var tipSeen by remember {
        mutableStateOf(App.sharedPreferences.getBoolean(SubforumTipSeenKey, false))
    }
    var contextMenuForum by remember { mutableStateOf<Forum?>(null) }

    fun setSubforumFilter(show: Boolean, subforum: Subforum) {
        // Optimistic local flip; the server response reconciles it.
        val index = subforums.indexOfFirst { it.forum.id == subforum.forum.id }
        if (index >= 0) {
            subforums[index] = subforums[index].toBuilder().setSelected(show).build()
        }
        val parentFid = if (forumId.hasFid()) forumId.fid else forumId.stid
        scope.launch {
            val result = logicCallAsync(
                AsyncRequest.newBuilder()
                    .setSubforumFilter(
                        SubforumFilterRequest.newBuilder()
                            .setOperation(
                                if (show) {
                                    SubforumFilterRequest.Operation.SHOW
                                } else {
                                    SubforumFilterRequest.Operation.BLOCK
                                }
                            )
                            .setForumId(parentFid)
                            .setSubforumFilterId(subforum.filterId)
                            .build()
                    )
                    .build(),
                SubforumFilterResponse.parser(),
            )
            result.onSuccess {
                Haptics.play(view, Haptics.NotificationType.SUCCESS)
                dataSource.refresh(silentOnError = true)
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            SmallTopAppBar(
                color = Color.Transparent, // v1.8.5: no solid top band
                title = L.str(context, "Subforums of %@", forumName),
            )
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = state.isRefreshing,
            onRefresh = { dataSource.refreshAsync(sleepMillis = 500) },
            modifier = Modifier.fillMaxSize().padding(padding)
                .then(rememberScreenBackdropSourceModifier()),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!tipSeen) {
                    item(key = "tip") {
                        SubforumTipCard(onDismiss = {
                            tipSeen = true
                            App.sharedPreferences.edit()
                                .putBoolean(SubforumTipSeenKey, true)
                                .apply()
                        })
                    }
                }
                if (dataSource.isInitialLoading && subforums.isEmpty()) {
                    item(key = "loading") {
                        Box(Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).align(Alignment.Center),
                                strokeWidth = 2.dp,
                            )
                        }
                    }
                } else if (subforums.isEmpty()) {
                    item(key = "empty") { ListPlaceholder(L.str(context, "Empty")) }
                } else {
                    itemsIndexed(
                        subforums,
                        key = { _, subforum ->
                            "${forumIdKey(subforum.forum.id)}:${subforum.filterId}"
                        },
                    ) { _, subforum ->
                        val forum = subforum.forum
                        val isFavorite = favoriteForums.any { it.id == forum.id }
                        Box {
                            // Miuix Card's native click/long-press handling
                            // replaces the Material Surface + combinedClickable
                            // container (same approach as ForumRowCard); the
                            // row's old padding becomes the card's insideMargin.
                            Card(
                                modifier = Modifier.fillMaxWidth().sharedContainer(
                                    // Morph this row into the subforum's
                                    // topic list (SharedNav shared
                                    // container; key = destination key).
                                    key = RouteDestination.keyFor(
                                        Route.TopicList(forumId = forum.id)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = null,
                                ),
                                cornerRadius = 12.dp,
                                insideMargin = PaddingValues(
                                    start = 16.dp,
                                    top = 12.dp,
                                    bottom = 12.dp,
                                    end = 8.dp,
                                ),
                                onClick = { navigator.push(Route.TopicList(forumId = forum.id)) },
                                onLongPress = { contextMenuForum = forum },
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        ForumRow(
                                            forum = forum,
                                            isFavorite = isFavorite,
                                        )
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Switch(
                                        checked = subforum.filterable && subforum.selected ||
                                            !subforum.filterable,
                                        onCheckedChange = { checked ->
                                            if (subforum.filterable) {
                                                setSubforumFilter(checked, subforum)
                                            }
                                        },
                                        enabled = subforum.filterable,
                                    )
                                }
                            }

                            // Miuix menu (phase 3): the single star item
                            // is a MiuixMenuItem with a leading icon -
                            // Miuix 0.8.8's own DropdownImpl rows cannot
                            // carry one (see ui/components/MiuixMenu.kt).
                            MiuixDropdownMenu(
                                expanded = contextMenuForum == forum,
                                onDismissRequest = { contextMenuForum = null },
                                alignment = PopupPositionProvider.Align.Start,
                            ) {
                                MiuixMenuItem(
                                    text = L.str(
                                        context,
                                        if (isFavorite) {
                                            "Remove from Favorites"
                                        } else {
                                            "Mark as Favorite"
                                        },
                                    ),
                                    icon = if (isFavorite) Icons.Outlined.Star
                                    else Icons.Outlined.StarBorder,
                                    onClick = {
                                        App.favoriteForums.toggle(forum) {
                                            Haptics.lightImpact(view)
                                        }
                                        contextMenuForum = null
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** First-visit tip card, the TipKit `SubforumListTip` equivalent. */
@Composable
private fun SubforumTipCard(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Outlined.Category,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    L.str(context, "View and Manage Subforums"),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    L.str(context, "Subforum Tip"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Outlined.Close, contentDescription = null)
            }
        }
    }
}
