package com.bugenzhao.mnga.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.model.PagingDataSource

/**
 * Standard paged list scaffolding: pull-to-refresh, initial loading spinner,
 * empty/error placeholders, footer and last-3-items prefetch.
 */
@Composable
fun <Item : Any> PagedList(
    dataSource: PagingDataSource<*, Item>,
    key: ((Item) -> Any)? = null,
    showFooter: Boolean = true,
    showInitialLoading: Boolean = true,
    emptyPlaceholder: String = "No Results",
    header: (@Composable () -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
    /** True when this list was freshly entered (not a return from a pushed
     * screen). The saveable scroll position is restored from the route
     * registry even after a fresh push, so it is explicitly reset to the top
     * in that case. */
    freshEntry: Boolean = false,
    /** Bump to reset the scroll position to the top (e.g. a toolbar
     * "refresh" action that also wants to show the newest items). */
    scrollToTopSignal: Int = 0,
    /**
     * Grouped-card mode (v1.8.0 MIUIX layout): the items render as one
     * continuous large-corner card — the list paints the card surface
     * behind the run of (flat) rows with hairline dividers between
     * them, instead of one card per row with gaps. The header and
     * footer stay outside the card. As pages append, a row that was
     * the card's rounded bottom simply becomes a middle row.
     */
    groupedCard: Boolean = false,
    itemContent: @Composable (Int, Item) -> Unit,
) {
    val state by dataSource.state.collectAsState()
    // Save the scroll position so returning to this screen restores it.
    val listState = rememberSaveable(saver = LazyListState.Saver) { LazyListState() }

    // Reset the scroll position on a fresh entry.
    LaunchedEffect(freshEntry) {
        if (freshEntry) listState.scrollToItem(0)
    }
    // Reset the scroll position when the host signals a refresh.
    LaunchedEffect(scrollToTopSignal) {
        if (scrollToTopSignal > 0) listState.scrollToItem(0)
    }

    // Prefetch when approaching the end.
    LaunchedEffect(listState, state.items.size) {
        snapshotFlow {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.items.size - 3
        }.collect { nearEnd ->
            if (nearEnd && state.items.isNotEmpty()) {
                dataSource.loadMoreIfNeeded(state.items.size - 1)
            }
        }
    }

    top.yukonga.miuix.kmp.basic.PullToRefresh(
        isRefreshing = state.isRefreshing,
        onRefresh = { dataSource.refreshAsync(sleepMillis = 500) },
        modifier = Modifier.fillMaxSize(),
    ) {
        when {
            dataSource.isInitialLoading && showInitialLoading ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            dataSource.isInitialLoading ->
                // The host screen shows its own (e.g. toolbar) loading
                // indicator; keep the content area blank until data arrives.
                Box(Modifier.fillMaxSize())
            state.items.isEmpty() && state.latestError != null ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    val err = state.latestError
                    if (err != null) ErrorPlaceholder(err) { dataSource.refresh() }
                }
            state.items.isEmpty() ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ListPlaceholder(emptyPlaceholder)
                }
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = contentPadding,
                verticalArrangement = if (groupedCard) {
                    Arrangement.spacedBy(0.dp)
                } else {
                    Arrangement.spacedBy(8.dp)
                },
            ) {
                if (header != null) {
                    item(key = "header") { header() }
                }
                itemsIndexed(state.items, key = if (key != null) { _, item -> key(item) } else null) { index, item ->
                    if (groupedCard) {
                        val isFirst = index == 0
                        val isLast = index == state.items.lastIndex
                        val cardShape = when {
                            isFirst && isLast -> RoundedCornerShape(24.dp)
                            isFirst -> RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp,
                            )
                            isLast -> RoundedCornerShape(
                                bottomStart = 24.dp, bottomEnd = 24.dp,
                            )
                            else -> RoundedCornerShape(0.dp)
                        }
                        Column(
                            Modifier.fillMaxWidth().clip(cardShape)
                                .background(MiuixTheme.colorScheme.surfaceContainer),
                        ) {
                            itemContent(index, item)
                            if (!isLast) {
                                top.yukonga.miuix.kmp.basic.HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MiuixTheme.colorScheme.dividerLine,
                                )
                            }
                        }
                    } else {
                        itemContent(index, item)
                    }
                }
                if (showFooter) {
                    item(key = "footer") {
                        AdaptiveFooter(loading = state.isLoading, noMore = !dataSource.hasMore)
                    }
                }
            }
        }
    }
}
