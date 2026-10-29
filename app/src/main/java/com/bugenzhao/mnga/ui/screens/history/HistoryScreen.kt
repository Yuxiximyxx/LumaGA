package com.bugenzhao.mnga.ui.screens.history

import androidx.compose.ui.graphics.Color


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import com.bugenzhao.mnga.logicCallAsync
import com.bugenzhao.mnga.protos.datamodel.CacheOperation
import com.bugenzhao.mnga.protos.datamodel.CacheType
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.protos.service.CacheRequest
import com.bugenzhao.mnga.protos.service.CacheResponse
import com.bugenzhao.mnga.ui.components.AdaptiveFooter
import com.bugenzhao.mnga.ui.components.ErrorPlaceholder
import com.bugenzhao.mnga.ui.components.ListPlaceholder
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.screens.topiclist.TopicRow
import com.bugenzhao.mnga.ui.screens.topiclist.topicSubjectFull
import com.bugenzhao.mnga.util.L
import kotlinx.coroutines.launch


/**
 * Local reading history, a port of `TopicHistoryListView`: snapshots served
 * from cache, searched by committed text, rows show the last-visit timestamp,
 * and a clear-history action wipes the TOPIC_HISTORY cache.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navigator: Navigator) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Entry-scoped ViewModel: the loaded history survives pop-backs
    // (composition is disposed, ViewModel is not) — no refetch on return.
    val historyVM: HistoryViewModel = viewModel()
    val dataSource = historyVM.dataSource
    val state by dataSource.state.collectAsState()
    LaunchedEffect(dataSource) {
        if (dataSource.notLoaded) dataSource.initialLoad()
    }

    // BasicSearchModel semantics: committed on submit, nil when cleared.
    var searchText by remember { mutableStateOf("") }
    var committedText by remember { mutableStateOf<String?>(null) }
    var searchActive by remember { mutableStateOf(false) }
    var showingClearConfirmation by remember { mutableStateOf(false) }

    fun clearHistory() {
        scope.launch {
            val result = logicCallAsync(
                AsyncRequest.newBuilder()
                    .setCache(
                        CacheRequest.newBuilder()
                            .setType(CacheType.TOPIC_HISTORY)
                            .setOperation(CacheOperation.CLEAR)
                            .build()
                    )
                    .build(),
                CacheResponse.parser(),
            )
            result.onSuccess {
                dataSource.refresh()
            }
        }
    }

    val visibleItems = state.items

    // Optimistically hidden rows after swipe-delete (no full refresh, so no
    // pull-to-refresh flash; the row below slides up via animateItem).
    val hiddenIds = remember { mutableStateListOf<String>() }

    // Snapshot display topic: dates replaced by the visit timestamp (ms -> s).
    val displayTopics = visibleItems.mapNotNull { snapshot ->
        val topic = snapshot.topicSnapshot
        if (topic.id.isEmpty()) return@mapNotNull null
        if (topic.id in hiddenIds) return@mapNotNull null
        val visitDate = snapshot.timestamp / 1000
        topic.toBuilder()
            .setPostDate(visitDate)
            .setLastPostDate(visitDate)
            .build()
    }
    val filteredTopics = committedText?.let { text ->
        displayTopics.filter { topicSubjectFull(it).contains(text) }
    } ?: displayTopics

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            SmallTopAppBar(
                color = Color.Transparent, // v1.8.5: no solid top band
                title = L.str(context, "History"),
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { searchActive = !searchActive }) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = L.str(context, "Search History"),
                        )
                    }
                    IconButton(onClick = { showingClearConfirmation = true }) {
                        Icon(Icons.Outlined.Delete, contentDescription = L.str(context, "Delete"))
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (searchActive) {
                TextField(
                    value = searchText,
                    onValueChange = { value ->
                        searchText = value
                        if (value.isEmpty()) committedText = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    label = L.str(context, "Search History"),
                    useLabelAsPlaceholder = true,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            searchText = ""
                            committedText = null
                            searchActive = false
                        }) {
                            Icon(Icons.Outlined.Close, contentDescription = null)
                        }
                    },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = { committedText = searchText.takeIf { it.isNotEmpty() } },
                    ),
                )
            }

            val listState = rememberLazyListState()
            LaunchedEffect(listState, filteredTopics.size) {
                snapshotFlow {
                    val info = listState.layoutInfo
                    val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
                    last >= filteredTopics.size - 3
                }.collect { nearEnd ->
                    if (nearEnd && filteredTopics.isNotEmpty()) {
                        dataSource.loadMoreIfNeeded(filteredTopics.size - 1)
                    }
                }
            }

            PullToRefresh(
                isRefreshing = state.isRefreshing,
                onRefresh = { dataSource.refreshAsync(sleepMillis = 500) },
                modifier = Modifier.fillMaxSize(),
            ) {
                when {
                    dataSource.isInitialLoading ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    displayTopics.isEmpty() && state.latestError != null ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ErrorPlaceholder(state.latestError!!) { dataSource.refresh() }
                        }
                    displayTopics.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ListPlaceholder(L.str(context, "No History"))
                        }
                    committedText != null && filteredTopics.isEmpty() ->
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            ListPlaceholder(L.str(context, "No Results"))
                        }
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        itemsIndexed(filteredTopics, key = { _, topic -> topic.id }) { _, topic ->
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    if (value == SwipeToDismissBoxValue.StartToEnd ||
                                        value == SwipeToDismissBoxValue.EndToStart
                                    ) {
                                        // Optimistic: hide immediately for a smooth
                                        // slide-up; un-hide if the RPC fails.
                                        hiddenIds.add(topic.id)
                                        historyVM.deleteTopic(topic.id) {
                                            hiddenIds.remove(topic.id)
                                        }
                                        true
                                    } else {
                                        false
                                    }
                                },
                            )
                            SwipeToDismissBox(
                                state = dismissState,
                                modifier = Modifier.animateItem(),
                                backgroundContent = {
                                    val alignment = when (dismissState.dismissDirection) {
                                        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                                        else -> Alignment.CenterEnd
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MiuixTheme.colorScheme.errorContainer)
                                            .padding(horizontal = 20.dp),
                                        contentAlignment = alignment,
                                    ) {
                                        Icon(
                                            Icons.Outlined.Delete,
                                            contentDescription = L.str(context, "Delete"),
                                            tint = MiuixTheme.colorScheme.onErrorContainer,
                                        )
                                    }
                                },
                            ) {
                                TopicRow(
                                    topic = topic,
                                    dimmedSubject = false,
                                    sharedContainerKey = "topic-details/${topic.id}",
                                    onClick = {
                                        navigator.push(
                                            Route.TopicDetails(
                                                topicId = topic.id,
                                                fav = topic.fav.takeIf { it.isNotEmpty() },
                                            )
                                        )
                                    },
                                )
                            }
                        }
                        item(key = "footer") {
                            AdaptiveFooter(loading = state.isLoading, noMore = !dataSource.hasMore)
                        }
                    }
                }
            }
        }
    }

    if (showingClearConfirmation) {
        WindowDialog(
            show = true,
            title = L.str(context, "Are you sure to clear the cache?"),
            summary = L.str(context, "Topic Histories"),
            onDismissRequest = { showingClearConfirmation = false },
        ) {
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = L.str(context, "Cancel"),
                    onClick = { showingClearConfirmation = false },
                )
                TextButton(
                    text = L.str(context, "Clear"),
                    onClick = {
                        showingClearConfirmation = false
                        clearHistory()
                    },
                )
            }
        }
    }
}
