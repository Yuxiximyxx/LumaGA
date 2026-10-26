package com.bugenzhao.mnga.ui.screens.user

import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.bugenzhao.mnga.ui.root.ScreenBackdropProvider
import com.bugenzhao.mnga.ui.root.rememberScreenBackdropSourceModifier
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.model.PlusFeature
import com.bugenzhao.mnga.model.ViewingImageModel
import com.bugenzhao.mnga.model.appScope
import com.bugenzhao.mnga.protos.datamodel.PostContent
import com.bugenzhao.mnga.protos.datamodel.Topic
import com.bugenzhao.mnga.protos.datamodel.TopicWithLightPost
import com.bugenzhao.mnga.protos.datamodel.User
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.storage.BlockWordsStorage
import com.bugenzhao.mnga.ui.components.DateTimeText
import com.bugenzhao.mnga.ui.components.MiuixDropdownMenu
import com.bugenzhao.mnga.ui.components.MiuixMenuItem
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.screens.topiclist.TopicRow
import com.bugenzhao.mnga.util.L
import com.bugenzhao.mnga.util.URLs
import com.xah.container.component.base.sharedContainer
import java.net.URLEncoder
import kotlinx.coroutines.launch

private enum class ProfileTab(val labelKey: String) {
    TOPICS("Topics"),
    POSTS("Posts"),
}

/**
 * Full user profile page, ported from `UserProfileView` / `RemoteUserProfileView`:
 * huge user header, signature, and a Topics/Posts segmented paged list.
 */
@OptIn(ExperimentalMaterial3Api::class)
/**
 * Entry point: publishes this screen's backdrop (v1.7.6) — recorded by
 * the content root below for the screen's whole lifetime, sampled by
 * its popup menu from the Scaffold's host (see ScreenBackdrop.kt).
 */
@Composable
fun UserProfileScreen(
    navigator: Navigator,
    userId: String? = null,
    userName: String? = null,
    user: User? = null,
    signatureContent: (@Composable (PostContent) -> Unit)? = null,
    onNewMessage: (User) -> Unit = { },
) {
    val screenBackdrop = rememberLayerBackdrop()
    ScreenBackdropProvider(screenBackdrop) {
        UserProfileScreenContent(
            navigator, userId, userName, user, signatureContent, onNewMessage,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UserProfileScreenContent(
    navigator: Navigator,
    userId: String? = null,
    userName: String? = null,
    user: User? = null,
    signatureContent: (@Composable (PostContent) -> Unit)? = null,
    onNewMessage: (User) -> Unit = { },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Entry-scoped ViewModel: the resolved user and the paged topic/post
    // lists survive pop-backs (composition is disposed, ViewModel is not),
    // so returning does not re-resolve the user nor refetch the lists.
    val profileVM: UserProfileViewModel = viewModel(
        factory = UserProfileViewModel.factory(userId, userName, user),
    )
    val currentUser by profileVM.currentUser.collectAsState()
    val loading by profileVM.loading.collectAsState()

    val authInfo by App.authStorage.authInfo.collectAsState()
    val blockWords by App.blockWords.words.collectAsState()

    val resolvedUser = currentUser
    val viewingImage = remember { ViewingImageModel() }

    val signaturePostModel = remember { SignaturePostModel(appScope) }
    val sentSignature by signaturePostModel.sent.collectAsState()
    var showSignatureEditor by remember { mutableStateOf(false) }

    val tab = remember { mutableStateOf(ProfileTab.TOPICS) }

    if (resolvedUser == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (loading) CircularProgressIndicator()
            else Text(
                L.str(context, "Empty"),
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
        return
    }

    val isMyself = resolvedUser.id.isNotEmpty() &&
        resolvedUser.id == authInfo.uid
    val blocked = resolvedUser.id.isNotEmpty() &&
        blockWords.contains(BlockWordsStorage.fromUser(resolvedUser.name))
    val shouldShowList =
        resolvedUser.id.isNotEmpty() && !resolvedUser.isAnonymousUser && !blocked

    val topicDataSource = profileVM.topicDataSource(resolvedUser.id)
    val postDataSource = profileVM.postDataSource(resolvedUser.id)

    LaunchedEffect(topicDataSource) {
        if (topicDataSource.notLoaded) topicDataSource.initialLoad()
    }
    LaunchedEffect(postDataSource, tab.value) {
        if (tab.value == ProfileTab.POSTS && postDataSource.notLoaded) {
            postDataSource.initialLoad()
        }
    }

    fun reloadUser() {
        profileVM.reload()
    }
    fun refresh() {
        scope.launch {
            profileVM.reload()
            if (shouldShowList) {
                topicDataSource.refreshAsync()
                postDataSource.refreshAsync()
            }
        }
    }
    // Reload after the signature update round-trips (own profile only).
    LaunchedEffect(sentSignature) {
        if (sentSignature != null && isMyself) reloadUser()
    }

    val displayName = resolvedUser.name.displayString
    val title =
        if (resolvedUser.isAnonymousUser) L.str(context, "Anonymous User")
        else displayName.ifEmpty { L.str(context, "User Profile") }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            // Miuix 0.8.8 SmallTopAppBar takes a plain String title and has
            // no subtitle slot; the per-tab subtitle ("%@'s Topics/Posts")
            // is not lost - the same string is already rendered in the list
            // as the SectionHeader below.
            SmallTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    ProfileOverflowMenu(
                        isMyself = isMyself,
                        anonymous = resolvedUser.isAnonymousUser,
                        blocked = blocked,
                        user = resolvedUser,
                        onEditSignature = { showSignatureEditor = true },
                        onNewMessage = {
                            if (checkPlusFeature(PlusFeature.SHORT_MESSAGE)) onNewMessage(resolvedUser)
                        },
                        onBlockUser = { App.blockWords.toggleUser(resolvedUser.name) },
                    )
                },
            )
        },
    ) { padding ->
        val topicState by topicDataSource.state.collectAsState()
        val postState by postDataSource.state.collectAsState()
        val listState = rememberLazyListState()

        // Force a fresh measure of the list whenever the active tab's content
        // transitions between loading/empty/loaded. Without this, the
        // LazyColumn occasionally keeps measuring the stale item set (the
        // item-provider update can land after the layout pass and no new
        // pass is scheduled), leaving the rows invisible until the next
        // recomposition (e.g. a tab switch) forces a relayout.
        val activeDS = if (tab.value == ProfileTab.TOPICS) topicDataSource else postDataSource
        val activeIsEmpty =
            if (tab.value == ProfileTab.TOPICS) topicState.items.isEmpty()
            else postState.items.isEmpty()

        // v1.7.7: the recreation above is driven by [measureEpoch], which
        // only advances on SAME-tab load-state transitions. It used to key
        // on the load-state pair directly — but a tab switch changes the
        // pair too (it now describes the other tab's data source), so
        // every switch recreated the whole LazyColumn and the topics/posts
        // swap was a hard cut. A tab switch forces a relayout on its own
        // (per the note above), so the workaround is not needed for it;
        // instead the tab-specific items below crossfade via animateItem
        // inside the surviving list. Specs mirror TabTransitionEffect's
        // asymmetric timing (enter 280ms / exit 220ms, easeOut).
        var measureEpoch by remember { mutableIntStateOf(0) }
        var lastTab by remember { mutableStateOf(tab.value) }
        var lastLoadPair by remember { mutableStateOf<Pair<Boolean, Boolean>?>(null) }
        val loadPair = activeDS.isInitialLoading to activeIsEmpty
        SideEffect {
            val prev = lastLoadPair
            if (prev != null && tab.value == lastTab && loadPair != prev) {
                measureEpoch++
            }
            lastTab = tab.value
            lastLoadPair = loadPair
        }
        val tabFadeInSpec = remember { tween<Float>(280, easing = EaseOut) }
        val tabFadeOutSpec = remember { tween<Float>(220, easing = EaseOut) }

        // Prefetch when approaching the end of the active list.
        val activeItems = if (tab.value == ProfileTab.TOPICS) topicState.items
        else postState.items
        LaunchedEffect(listState, activeItems.size) {
            snapshotFlow {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= activeItems.size - 3
            }.collect { nearEnd ->
                if (nearEnd && activeItems.isNotEmpty()) {
                    if (tab.value == ProfileTab.TOPICS) topicDataSource.loadMoreIfNeeded(activeItems.size - 1)
                    else postDataSource.loadMoreIfNeeded(activeItems.size - 1)
                }
            }
        }

        PullToRefresh(
            isRefreshing = topicState.isRefreshing || postState.isRefreshing,
            onRefresh = { refresh() },
            modifier = Modifier.fillMaxSize().padding(padding)
                .then(rememberScreenBackdropSourceModifier()),
        ) {
            key(measureEpoch) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "header") {
                    // 顶部间距与卡片左右间距一致（16dp），原标题位不再占额外空间。
                    UserProfileHeader(
                            user = resolvedUser,
                            blocked = blocked,
                            isMyself = isMyself,
                            onAvatarClick = { url -> viewingImage.show(url) },
                            onEditSignature = { showSignatureEditor = true },
                            onNewMessage = {
                                if (checkPlusFeature(PlusFeature.SHORT_MESSAGE)) {
                                    onNewMessage(resolvedUser)
                                }
                            },
                            onBlockUser = { App.blockWords.toggleUser(resolvedUser.name) },
                            onShare = { shareUser(context, resolvedUser) },
                            signatureContent = signatureContent,
                        )
                }

                if (!shouldShowList) {
                    if (blocked) {
                        item(key = "blocked") {
                            SectionCard {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                    contentAlignment = Alignment.Center,
                                ) { Text(L.str(context, "Blocked")) }
                            }
                        }
                    }
                } else {
                    item(key = "tabs") {
                        TabRow(
                            tabs = ProfileTab.entries.map { L.str(context, it.labelKey) },
                            selectedTabIndex = ProfileTab.entries.indexOf(tab.value),
                            onTabSelected = { tab.value = ProfileTab.entries[it] },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    if (tab.value == ProfileTab.TOPICS) {
                        item(key = "topics-header") {
                            Box(Modifier.animateItem(tabFadeInSpec, null, tabFadeOutSpec)) {
                                SectionHeader(L.str(context, "%@'s Topics", displayName))
                            }
                        }
                        if (topicDataSource.isInitialLoading) {
                            item(key = "topics-loading") {
                                Box(Modifier.animateItem(tabFadeInSpec, null, tabFadeOutSpec)) {
                                    CenteredLoading()
                                }
                            }
                        } else if (topicState.items.isEmpty()) {
                            item(key = "topics-empty") {
                                SectionCard(
                                    modifier = Modifier.animateItem(
                                        tabFadeInSpec, null, tabFadeOutSpec,
                                    ),
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text(L.str(context, "Empty")) }
                                }
                            }
                        } else {
                            itemsIndexed(topicState.items, key = { _, t -> "t-${t.id}" }) { _, topic ->
                                // The shared list row, not a `SectionCard`
                                // wrapper: it brings its own card surface.
                                TopicRow(
                                    topic = topic,
                                    modifier = Modifier.animateItem(
                                        tabFadeInSpec, null, tabFadeOutSpec,
                                    ),
                                    // Every topic here is this user's own, so
                                    // the posting date is what distinguishes
                                    // them, not the last reply.
                                    useTopicPostDate = true,
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
                    } else {
                        item(key = "posts-header") {
                            Box(Modifier.animateItem(tabFadeInSpec, null, tabFadeOutSpec)) {
                                SectionHeader(L.str(context, "%@'s Posts", displayName))
                            }
                        }
                        if (postDataSource.isInitialLoading) {
                            item(key = "posts-loading") {
                                Box(Modifier.animateItem(tabFadeInSpec, null, tabFadeOutSpec)) {
                                    CenteredLoading()
                                }
                            }
                        } else if (postState.items.isEmpty()) {
                            item(key = "posts-empty") {
                                SectionCard(
                                    modifier = Modifier.animateItem(
                                        tabFadeInSpec, null, tabFadeOutSpec,
                                    ),
                                ) {
                                    Box(
                                        Modifier.fillMaxWidth().padding(vertical = 20.dp),
                                        contentAlignment = Alignment.Center,
                                    ) { Text(L.str(context, "Empty")) }
                                }
                            }
                        } else {
                            itemsIndexed(postState.items, key = { _, tp -> "p-${tp.post.id.pid}" }) { _, tp ->
                                // The card is the tapped element: morph it
                                // into the topic-details page (SharedNav
                                // shared container; key = destination key).
                                SectionCard(
                                    modifier = Modifier.animateItem(
                                        tabFadeInSpec, null, tabFadeOutSpec,
                                    ).sharedContainer(
                                        key = "topic-details/${tp.topic.id}",
                                        shape = RoundedCornerShape(12.dp),
                                        containerColor = null,
                                    ),
                                ) {
                                    UserPostRow(tp) {
                                        // The original page is unknown (iOS FIXME).
                                        navigator.push(
                                            Route.TopicDetails(
                                                topicId = tp.topic.id,
                                                postId = tp.post.id.pid.takeIf { it.isNotEmpty() },
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "footer") {
                        val active = if (tab.value == ProfileTab.TOPICS) topicState else postState
                        val ds = if (tab.value == ProfileTab.TOPICS) topicDataSource else postDataSource
                        com.bugenzhao.mnga.ui.components.AdaptiveFooter(
                            loading = active.isLoading,
                            noMore = !ds.hasMore,
                        )
                    }
                }
            }
            }
        }
    }

    val editorShowing by signaturePostModel.showEditor.collectAsState()
    LaunchedEffect(showSignatureEditor, editorShowing) {
        if (showSignatureEditor && !editorShowing) {
            signaturePostModel.show(
                UserSignatureEditTask(
                    UserSignatureEditAction(
                        userID = resolvedUser.id,
                        initialSignature = resolvedUser.signature.rawReplacingBr,
                    )
                )
            )
        }
    }
    LaunchedEffect(editorShowing) {
        if (!editorShowing) showSignatureEditor = false
    }
    if (editorShowing) {
        SignatureEditorSheet(model = signaturePostModel) { showSignatureEditor = false }
    }

    ImageViewerOverlay(viewingImage)

}

@Composable
private fun ProfileOverflowMenu(
    isMyself: Boolean,
    anonymous: Boolean,
    blocked: Boolean,
    user: User,
    onEditSignature: () -> Unit,
    onNewMessage: () -> Unit,
    onBlockUser: () -> Unit,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val webURL = userWebURL(user)

    // Miuix menu (phase 3) replacing the Material DropdownMenu; the
    // Box anchors the popup to this button (see MiuixMenu.kt).
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Filled.MoreHoriz, contentDescription = L.str(context, "More"))
        }
        MiuixDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (isMyself) {
                MiuixMenuItem(
                    text = L.str(context, "Edit Signature"),
                    icon = Icons.Outlined.EditNote,
                    onClick = { expanded = false; onEditSignature() },
                )
            }
            if (!isMyself) {
                if (!anonymous) {
                    MiuixMenuItem(
                        text = L.str(context, "New Short Message"),
                        icon = Icons.AutoMirrored.Filled.Chat,
                        onClick = { expanded = false; onNewMessage() },
                    )
                }
                MiuixMenuItem(
                    text = L.str(context, if (blocked) "Unblock This User" else "Block This User"),
                    icon = Icons.Outlined.FrontHand,
                    // Destructive entry in error color (icon tinted too,
                    // matching HyperOS menu convention).
                    contentColor = if (blocked) MiuixTheme.colorScheme.onSurface
                    else MiuixTheme.colorScheme.error,
                    onClick = { expanded = false; onBlockUser() },
                )
            }
            if (!anonymous) {
                MiuixMenuItem(
                    text = L.str(context, "LumaGA Link"),
                    icon = Icons.Filled.Share,
                    onClick = {
                        expanded = false
                        shareText(context, "mnga://user/${user.id}", user.name.displayString)
                    },
                )
                if (webURL != null) {
                    MiuixMenuItem(
                        text = L.str(context, "NGA Link"),
                        icon = Icons.Filled.Share,
                        onClick = {
                            expanded = false
                            shareText(context, webURL, user.name.displayString)
                        },
                    )
                    MiuixMenuItem(
                        text = L.str(context, "Open in Browser"),
                        icon = Icons.Outlined.OpenInBrowser,
                        onClick = {
                            expanded = false
                            App.openURL.open(
                                android.net.Uri.parse(webURL),
                                prefs = App.prefs,
                            )
                        },
                    )
                }
            }
        }
    }
}

/** Light post row: topic subject + content preview + date. */
@Composable
private fun UserPostRow(tp: TopicWithLightPost, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            tp.topic.subject.content.ifEmpty { tp.topic.subjectContent },
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        RawPostContent(
            tp.post.content,
            style = MaterialTheme.typography.bodyMedium,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        DateTimeText(tp.post.postDate)
    }
}

@Composable
internal fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MiuixTheme.colorScheme.surface,
    ) { Column { content() } }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    )
}

@Composable
private fun CenteredLoading() {
    Box(Modifier.fillMaxWidth().padding(vertical = 24.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(Modifier.padding(2.dp), strokeWidth = 2.dp)
    }
}

/** `mnga://user/<uid>`; suppressed for mock ids. */
internal fun userMNGAURL(user: User): String? =
    if (user.id.startsWith("mnga_")) null else "mnga://user/${user.id}"

/** NGA web profile link; suppressed for mock ids. */
internal fun userWebURL(user: User): String? = when {
    user.id.startsWith("mnga_") -> null
    user.id.isNotEmpty() -> URLs.base + "nuke.php?func=ucp&uid=" + user.id
    user.name.normal.isNotEmpty() ->
        URLs.base + "nuke.php?func=ucp&username=" +
            URLEncoder.encode(user.name.normal, "UTF-8")
    else -> null
}

internal fun shareUser(context: android.content.Context, user: User) {
    val url = userMNGAURL(user) ?: userWebURL(user) ?: return
    shareText(context, url, user.name.displayString)
}

internal fun shareText(context: android.content.Context, text: String, title: String?) {
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        title?.let { putExtra(android.content.Intent.EXTRA_SUBJECT, it) }
    }
    runCatching {
        context.startActivity(android.content.Intent.createChooser(intent, null))
    }
}

/** Minimal full-screen viewer for the huge avatar tap. */
@Composable
internal fun ImageViewerOverlay(model: ViewingImageModel) {
    val showing by model.showing.collectAsState()
    val urls by model.urls.collectAsState()
    val index by model.currentIndex.collectAsState()
    if (!showing) return
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { model.dismiss() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .clickable { model.dismiss() },
            contentAlignment = Alignment.Center,
        ) {
            coil.compose.AsyncImage(
                model = urls.getOrNull(index),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
