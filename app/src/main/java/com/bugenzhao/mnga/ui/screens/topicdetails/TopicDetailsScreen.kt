package com.bugenzhao.mnga.ui.screens.topicdetails

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SubdirectoryArrowRight
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.OpenInFull
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.rememberPullToRefreshState
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.logicCall
import com.bugenzhao.mnga.model.GenericPostModel
import com.bugenzhao.mnga.model.display
import com.bugenzhao.mnga.model.NavigationIdentifier
import com.bugenzhao.mnga.model.PlusFeature
import com.bugenzhao.mnga.model.PlusModel
import com.bugenzhao.mnga.model.QuotedPostResolver
import com.bugenzhao.mnga.model.ToastModel
import com.bugenzhao.mnga.model.TopicDetailsActionModel
import com.bugenzhao.mnga.model.TopicPostLocator
import com.bugenzhao.mnga.model.VotesModel
import com.bugenzhao.mnga.model.ViewingImageModel
import com.bugenzhao.mnga.model.appScope
import com.bugenzhao.mnga.protos.datamodel.ForumId
import com.bugenzhao.mnga.protos.datamodel.Post
import com.bugenzhao.mnga.protos.datamodel.PostId
import com.bugenzhao.mnga.protos.datamodel.Topic
import com.bugenzhao.mnga.protos.datamodel.postIdOrNull
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.protos.service.SyncRequest
import com.bugenzhao.mnga.protos.service.TopicDetailsRequest
import com.bugenzhao.mnga.protos.service.TopicDetailsResponse
import com.bugenzhao.mnga.protos.service.UpdateTopicProgressRequest
import com.bugenzhao.mnga.storage.TopicResumeFrom
import com.bugenzhao.mnga.ui.components.DateTimeText
import com.bugenzhao.mnga.ui.components.MiuixDropdownMenu
import com.bugenzhao.mnga.ui.components.liquidGlass
import com.bugenzhao.mnga.ui.components.MiuixMenuDivider
import com.bugenzhao.mnga.ui.components.MiuixMenuHeader
import com.bugenzhao.mnga.ui.components.MiuixMenuItem
import com.bugenzhao.mnga.ui.components.hotAccentColor
import com.bugenzhao.mnga.ui.components.toggleTopicFavor
import com.bugenzhao.mnga.ui.components.imageviewer.ImageViewerDialog
import com.bugenzhao.mnga.ui.editor.EditorController
import com.bugenzhao.mnga.ui.root.DockScheme
import com.bugenzhao.mnga.ui.root.GlassSpec
import com.bugenzhao.mnga.ui.root.LowResLayerBackdrop
import com.bugenzhao.mnga.ui.root.ScreenBackdropProvider
import com.bugenzhao.mnga.ui.root.glassContentColor
import com.bugenzhao.mnga.ui.root.lowResLayerBackdrop
import com.bugenzhao.mnga.ui.root.recordedPageBackground
import com.bugenzhao.mnga.ui.root.rememberGlassSettings
import com.bugenzhao.mnga.ui.root.rememberLowResLayerBackdrop
import com.kyant.backdrop.Backdrop
import com.bugenzhao.mnga.ui.editor.PostReplyTask
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.util.Constants
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import java.util.Date
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * v1.8.5-exp experiment knob: the linear resolution the topic-details
 * content recording runs at — 0.25 ⇒ the recording texture holds
 * 1/16 the pixels of the v1.8.2 full-resolution recording that
 * crashed heavy topics. If tid 47690428 still crashes on device at
 * 1/4, the next rung down is 1/8 — or the experiment is abandoned
 * and the v1.8.3 flat-color isolation restored. See
 * [LowResLayerBackdrop].
 */
private const val DETAILS_BACKDROP_SCALE = 0.25f

/**
 * Topic details reading screen, ported from `Shared/Views/TopicDetailsView.swift`.
 * Creates its paging data source, locator, quoted-post resolver, action model
 * and vote store, and reacts to every action-model flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailsScreen(
    navigator: Navigator,
    route: Route.TopicDetails,
    editor: EditorController? = null,
) {
    // This screen's backdrop doubles as the screen backdrop
    // (v1.7.6): the content root records it for the screen's whole
    // lifetime and every consumer — the inline reply bar and the
    // header round buttons beside the list, the popup menus (⋮ and
    // per-post) in the Scaffold's host — sits outside that
    // recording. (The page capsule that also sampled it was deleted
    // in v1.8.4.) Hoisted here so the provider can wrap the Scaffold
    // (see ScreenBackdrop.kt).
    //
    // v1.8.5-exp (experiment branch): the backdrop is a LOW-RES
    // recording — the content root records the real post content
    // into it at [DETAILS_BACKDROP_SCALE] linear resolution (see
    // LowResBackdrop.kt and the backdrop note in the content below).
    // Whether heavy topics survive that raster load is decided on
    // device (tid 47690428); until then the v1.8.3 flat isolation is
    // suspended, not repealed.
    val indicatorBackdrop = rememberLowResLayerBackdrop(DETAILS_BACKDROP_SCALE)
    ScreenBackdropProvider(indicatorBackdrop) {
        TopicDetailsScreenContent(navigator, route, editor, indicatorBackdrop)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicDetailsScreenContent(
    navigator: Navigator,
    route: Route.TopicDetails,
    editor: EditorController? = null,
    indicatorBackdrop: Backdrop,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    // Screen mode, derived from the route.
    val onlyPostId = route.postId?.takeIf { it.isNotEmpty() && !route.anonymousAuthorOnly }
    val authorOnlyMode = route.authorId != null || route.anonymousAuthorOnly
    val forceLocalMode = route.localCache
    val enableAuthorOnly = !forceLocalMode && !authorOnlyMode
    val mock = route.topicId.startsWith("mnga_")

    // Local favorite override (kept after the More-menu toggle), ahead of the
    // response-provided state.
    var favoredOverride by remember(route) { mutableStateOf<Boolean?>(null) }

    // Mutable topic mirror, refreshed from every response.
    var topic by remember(route) {
        mutableStateOf(
            Topic.newBuilder()
                .setId(route.topicId)
                .apply { route.fav?.let { fav = it } }
                .build()
        )
    }

    // Resume reading progress (Plus feature `resumeProgress`).
    var floorToJump by remember(route) { mutableStateOf<Int?>(null) }
    var postIdToJump by remember(route) { mutableStateOf<PostId?>(null) }
    val initialPage = remember(route) {
        var page = route.startPage ?: 1
        if (onlyPostId == null && route.startPage == null && !forceLocalMode) {
            // Local reading progress for favorited topics: jump back to where
            // the user left off. Saved on exit, no server round-trip needed.
            // Takes precedence over the server-based resume below.
            val savedFloor = App.prefs.getReadingFloor(route.topicId)?.takeIf { it >= 1 }
            android.util.Log.d("ReadingProgress", "restore: topicId=${route.topicId} savedFloor=$savedFloor")
            val initialFloor: Int? = if (savedFloor != null) {
                savedFloor
            } else {
                val resumeFrom = App.prefs.resumeTopicFrom
                when (resumeFrom) {
                    TopicResumeFrom.LAST ->
                        topic.lastViewingFloor.takeIf { topic.hasLastViewingFloor() && it >= 3 }
                            ?.plus(1)
                    TopicResumeFrom.HIGHEST ->
                        topic.highestViewedFloor.takeIf { topic.hasHighestViewedFloor() && it >= 3 }
                            ?.plus(1)
                    else -> null
                }
            }
            if (initialFloor != null) {
                floorToJump = initialFloor
                page = (initialFloor + Constants.postPerPage) / Constants.postPerPage
            }
        }
        page
    }

    // Data source over `topicDetails`, held by the entry-scoped ViewModel so
    // the loaded floors survive pop-backs (composition is disposed, ViewModel
    // is not) — no refetch on return.
    val topicDetailsVM: TopicDetailsViewModel = viewModel(
        factory = TopicDetailsViewModel.factory(route, initialPage),
    )
    val dataSource = topicDetailsVM.dataSource

    val action = remember(route) { TopicDetailsActionModel() }
    val postLocator = remember(route) { TopicPostLocator(scope, topic) }
    val votes = remember(route) { VotesModel() }
    val quotedPosts = remember(route) {
        QuotedPostResolver(scope).apply {
            localPostProvider = { id -> dataSource.items.firstOrNull { it.id == id } }
        }
    }
    val viewingImage = remember(route) { ViewingImageModel() }
    val currentViewingFloor = remember(route) { CurrentViewingFloor() }

    // Reading-progress persistence (audited & hardened for v1.8.0):
    // the local floor is now saved CONTINUOUSLY as the top visible
    // floor changes (the snapshotFlow effect further down), not only
    // from the dispose hook. The dispose-only save read the live
    // viewing set at teardown time — after the rows' own dispose
    // callbacks may already have emptied it — and never ran at all
    // when the process died while reading, so "reopen jumps back"
    // silently failed in exactly those cases. [lastVisibleFloor]
    // mirrors the latest value for the dispose-time save below.
    var lastVisibleFloor by remember(route) { mutableStateOf<Int?>(null) }

    // Posting flows (reply / quote / comment / edit / report) are performed by
    // the global editor controller, whose sheet the root hosts.
    val onPostAction: ((action: String, post: Post) -> Unit)? = remember(editor) {
        editor?.let { controller ->
            { postAction, post ->
                when (postAction) {
                    PostRowAction.REPLY -> controller.reply(post)
                    PostRowAction.QUOTE -> controller.quote(post)
                    PostRowAction.COMMENT -> controller.comment(post)
                    PostRowAction.MODIFY -> controller.modify(post)
                    PostRowAction.REPORT -> controller.report(post)
                    else -> {}
                }
            }
        }
    }

    // Pull the just-posted reply into view: each task carries the page its
    // result lands on, so reload exactly that one once the send succeeds.
    val noSent = remember { MutableStateFlow<GenericPostModel.Context?>(null) }
    val sentContext by (editor?.postReply?.sent ?: noSent).collectAsState()
    LaunchedEffect(sentContext) {
        val task = sentContext?.task as? PostReplyTask ?: return@LaunchedEffect
        if (task.action.postIdOrNull?.tid != route.topicId) return@LaunchedEffect
        when (val page = task.pageToReload) {
            is GenericPostModel.PageToReload.Last -> dataSource.reloadLastPage()
            is GenericPostModel.PageToReload.Exact ->
                dataSource.reload(page = page.page, evenIfNotLoaded = true)
        }
    }

    val state by dataSource.state.collectAsState()
    val response = state.latestResponse as? TopicDetailsResponse
    val localMode = forceLocalMode || (response?.isLocalCache == true)

    val listState = rememberLazyListState()

    // Backdrop history (the recording this screen's consumers —
    // reply bar, header round buttons, menus — sample): originally
    // (v1.7.1) the whole content box below was recorded into
    // indicatorBackdrop and the consumers sampled the post content
    // itself. v1.8.1 crash isolation: since v1.7.1 some topics crash
    // on open (v1.7.0 opens them fine), and that content recording —
    // re-recording the richest subtree in the app (tables, inline
    // images, quote layers) into a graphics layer on every draw — is
    // the only content-dependent code the 1.7.1 window added to the
    // open path. The recording was therefore reduced to the page
    // background layer alone (the flat recording node that sat
    // inside the content box until this experiment): the reply bar /
    // header buttons / menus kept the shared glass material over a
    // flat page texture, and no post content entered a
    // graphics-layer recording from this screen.
    // v1.8.2 retried the single in-screen content recording (the glass
    // read flat without it) and the crash returned on device, so
    // v1.8.3 restored that isolation; the shell's dock backdrop
    // likewise never records while a TopicDetails is on top
    // (DockBackdrop.kt) — still true in the experiment below.
    //
    // v1.8.5-exp (this branch): the isolation is suspended for a
    // controlled retry. The content box records the REAL content
    // again — the v1.8.0/v1.8.2 geometry — but through
    // LowResLayerBackdrop at DETAILS_BACKDROP_SCALE (1/4 linear,
    // ~1/16 the pixels), cutting the recording's raster load while
    // keeping its draw-op count. Experiment in progress: the
    // verdict comes from the device (tid 47690428). If it crashes
    // again, this recording change is reverted wholesale and the
    // v1.8.3 flat isolation stands as the final form — do not
    // iterate the scale blindly without a device verdict.
    // (The backdrop itself is remembered by the entry point above,
    // which also publishes it as this screen's menu/toast sampling
    // source.)
    val glass = rememberGlassSettings()

    // Jump selector state.
    var showJumpSelector by remember { mutableStateOf(false) }
    var jumpSelectorMode by remember { mutableStateOf(TopicJumpSelectorMode.FLOOR) }
    val maxFloor = (response?.topic ?: topic).repliesNum

    // Reply chain / quoted replies overlays.
    val replyChain by action.showingReplyChain.collectAsState()
    val quotedReplies by action.showingQuotedReplies.collectAsState()
    val scrollToPid by action.scrollToPid.collectAsState()

    // First post (main floor).
    val items = state.items
    val first = remember(items) {
        items.minByOrNull { it.floor }?.takeIf { it.id.pid == "0" }
    }
    val blockWords by App.blockWords.words.collectAsState()
    fun isBlockedUserPost(post: Post): Boolean =
        App.users.cachedUser(post.authorId)?.let { user ->
            blockWords.contains(com.bugenzhao.mnga.storage.BlockWordsStorage.fromUser(user.name))
        } == true
    val visibleItems = remember(items, blockWords) {
        items.filterNot(::isBlockedUserPost)
    }
    val visibleFirst = remember(first, blockWords) {
        first?.takeUnless(::isBlockedUserPost)?.let { mainPost ->
            mainPost.toBuilder()
                .clearHotReplies()
                .addAllHotReplies(mainPost.hotRepliesList.filterNot(::isBlockedUserPost))
                .build()
        }
    }
    val atForum = remember(response, items) {
        val name = response?.forumName?.takeIf { it.isNotEmpty() } ?: return@remember null
        val fid = items.firstOrNull()?.fid?.takeIf { it.isNotEmpty() } ?: return@remember null
        com.bugenzhao.mnga.protos.datamodel.Forum.newBuilder()
            .setId(ForumId.newBuilder().setFid(fid))
            .setName(name)
            .build()
    }

    // Update the topic mirror + indexes whenever a response lands.
    LaunchedEffect(response) {
        val newTopic = response?.topic ?: return@LaunchedEffect
        if (onlyPostId == null && enableAuthorOnly) postLocator.seed(response.repliesList)
        quotedPosts.seed(response.repliesList)
        action.indexReplyRelations(response.repliesList)
        val mainFloor = response.repliesList.firstOrNull { it.id.pid == "0" }
        if (mainFloor != null && mainFloor.hotRepliesList.isNotEmpty()) {
            action.indexReplyRelations(mainFloor.hotRepliesList)
        }
        topic = topic.toBuilder().apply {
            if (id.isEmpty()) setId(newTopic.id)
            if (newTopic.hasParentForum()) setParentForum(newTopic.parentForum)
            setAuthorId(newTopic.authorId)
            setSubject(newTopic.subject)
            setPostDate(newTopic.postDate)
            setLastPostDate(newTopic.lastPostDate)
            setRepliesNum(newTopic.repliesNum)
            if (newTopic.hasRepliesNumLastVisit()) setRepliesNumLastVisit(newTopic.repliesNumLastVisit)
            if (newTopic.hasHighestViewedFloor()) setHighestViewedFloor(newTopic.highestViewedFloor)
            if (newTopic.hasLastViewingFloor()) setLastViewingFloor(newTopic.lastViewingFloor)
            if (newTopic.hasFav()) setFav(newTopic.fav)
            if (newTopic.isFavored) setIsFavored(true)
        }.build()
    }

    // Initial load.
    LaunchedEffect(Unit) { dataSource.initialLoad() }

    // XML parse error: optionally auto-open in the browser.
    LaunchedEffect(state.latestError) {
        val e = state.latestError ?: return@LaunchedEffect
        if (e.isXMLParseError && App.prefs.autoOpenInBrowserWhenBanned.value) {
            delay(750)
            openInBrowser(context, topic)
        }
    }

    // Scroll targets from the action model.
    val rows = remember(visibleItems, visibleFirst, response, atForum, onlyPostId) {
        buildRows(
            items = visibleItems,
            first = visibleFirst,
            showTail = shouldShowTailSection(dataSource, state, response, onlyPostId != null),
        )
    }
    val currentRows by rememberUpdatedState(rows)

    // v1.8.4: the page-indicator capsule and its current/total page
    // derived states are deleted wholesale (the user asked for the
    // capsule to go). Page turns stay on the swipe gesture, the
    // pull/load-back paths and the jump selector.
    LaunchedEffect(action, listState) {
        action.scrollToFloor.collect { floor ->
            if (floor != null) {
                // currentRows 始终是最新 rows（effect 不随 rows 重启，
                // 避免闭包过期导致加载前页后滚动目标找不到）。
                val index = currentRows.indexOfFirst { it.floor == floor }
                if (index >= 0) listState.animateScrollToItem(index)
                action.scrollToFloor.value = null
            }
        }
    }
    LaunchedEffect(action, rows) {
        action.scrollToPid.collect { pid ->
            if (pid != null) {
                val index = rows.indexOfFirst { it.pid == pid }
                if (index >= 0) listState.animateScrollToItem(index)
                action.scrollToPid.value = null
            }
        }
    }

    // After a refresh (incl. page jumps), scroll to the pending target.
    LaunchedEffect(state.lastRefreshTime) {
        if (state.lastRefreshTime == null) return@LaunchedEffect
        delay(500)
        val floor = floorToJump
        val pid = postIdToJump
        when {
            floor != null -> action.scrollToFloor.value = floor
            pid != null -> action.scrollToPid.value = pid.pid
        }
        floorToJump = null
        postIdToJump = null
    }

    // Navigation wiring for every action-model flow.
    LaunchedEffect(action) {
        action.navigateToTid.collect { tid ->
            if (tid != null) {
                navigator.push(Route.TopicDetails(topicId = tid))
                action.navigateToTid.value = null
            }
        }
    }
    LaunchedEffect(action) {
        action.navigateToPid.collect { pid ->
            if (pid != null) {
                navigator.push(Route.TopicDetails(topicId = "", postId = pid))
                action.navigateToPid.value = null
            }
        }
    }
    LaunchedEffect(action) {
        action.showUserProfile.collect { user ->
            if (user != null) {
                navigator.push(Route.UserProfile(user = user))
                action.showUserProfile.value = null
            }
        }
    }
    LaunchedEffect(action) {
        action.navigateToForum.collect { forum ->
            if (forum != null) {
                navigator.push(Route.TopicList(forumId = forum.id, categoryName = forum.name))
                action.navigateToForum.value = null
            }
        }
    }
    LaunchedEffect(action) {
        action.navigateToRemoteUserID.collect { uid ->
            if (uid != null) {
                navigator.push(Route.UserProfile(userId = uid))
                action.navigateToRemoteUserID.value = null
            }
        }
    }
    LaunchedEffect(action) {
        action.navigateToRemoteUserName.collect { name ->
            if (name != null) {
                navigator.push(Route.UserProfile(userName = name))
                action.navigateToRemoteUserName.value = null
            }
        }
    }
    LaunchedEffect(action) {
        action.navigateToAuthorOnly.collect { authorOnly ->
            if (authorOnly != null) {
                when (authorOnly) {
                    is TopicDetailsActionModel.AuthorOnly.ByUID ->
                        navigator.push(
                            Route.TopicDetails(
                                topicId = topic.id,
                                fav = route.fav,
                                authorId = authorOnly.uid,
                            )
                        )
                    is TopicDetailsActionModel.AuthorOnly.Anonymous ->
                        navigator.push(
                            Route.TopicDetails(
                                topicId = topic.id,
                                fav = route.fav,
                                anonymousAuthorOnly = true,
                                postId = authorOnly.postId?.pid,
                            )
                        )
                }
                action.navigateToAuthorOnly.value = null
            }
        }
    }
    LaunchedEffect(action) {
        action.navigateToLocalMode.collect { local ->
            if (local) {
                navigator.push(
                    Route.TopicDetails(
                        topicId = topic.id,
                        fav = route.fav,
                        postId = route.postId,
                        localCache = true,
                        startPage = 1,
                    )
                )
                action.navigateToLocalMode.value = false
            }
        }
    }

    // Continuous local save of the reading floor (see the note at
    // [lastVisibleFloor]): snapshotFlow only emits when the top
    // visible reply floor actually changes, and setReadingFloor is a
    // cheap SharedPreferences apply. Applies to every entry path —
    // favorites, home, forum lists, history, search, deep links all
    // funnel into this one screen.
    LaunchedEffect(listState, dataSource) {
        snapshotFlow {
            currentRows.drop(listState.firstVisibleItemIndex)
                .firstOrNull { it is RowSpec.Reply }
                ?.let { (it as RowSpec.Reply).post.floor }
        }.collect { floor ->
            if (floor != null && floor >= 1) {
                lastVisibleFloor = floor
                if (!mock && topic.id.isNotEmpty()) {
                    App.prefs.setReadingFloor(topic.id, floor)
                }
            }
        }
    }

    // Sync reading progress when leaving the screen.
    DisposableEffect(route) {
        onDispose {
            syncTopicProgress(topic, currentViewingFloor)
            // Remember where the user left off, so reopening jumps straight
            // back to that floor. Saved for all topics (capped at 300).
            // Prefer the continuously tracked floor: the live viewing
            // set may already be emptied by row teardown at this point.
            val floor = lastVisibleFloor ?: currentViewingFloor.currentLowest
            android.util.Log.d("ReadingProgress", "save: topicId=${topic.id} fav=${topic.fav} floor=$floor")
            floor?.let {
                App.prefs.setReadingFloor(topic.id, it)
            }
        }
    }

    // Locate a post within this topic (from rows' "Locate This Floor").
    fun locatePostInCurrentTopic(post: Post) {
        scope.launch {
            val result = postLocator.locate(post.id).await()
            result.fold(
                onSuccess = { location ->
                    action.scrollToFloor.value = null
                    action.scrollToPid.value = null
                    floorToJump = null
                    if (dataSource.items.any { it.id == post.id }) {
                        action.scrollToPid.value = post.id.pid
                    } else {
                        postIdToJump = post.id
                        dataSource.loadFromPage = location.page
                    }
                },
                onFailure = { e ->
                    ToastModel.showAuto(ToastModel.Message.Error(e.message ?: "error"))
                },
            )
        }
    }
    val childLocateFloor: ((Post) -> Unit)? =
        if (forceLocalMode || onlyPostId != null) null else { post -> locatePostInCurrentTopic(post) }

    // Floating reply bar (v1.8.0, per the confirmed mock): offered
    // under exactly the conditions of the More menu's Reply entry.
    val showReplyBar = onlyPostId == null && !mock && first != null && editor != null

    // Inline reply state (v1.8.3): the bar is a REAL text field in
    // this window now, not a tap target that opens the editor sheet —
    // two rounds of sheet focus/IME fixes never made the sheet's field
    // accept input on device ("tap animates, nothing can be typed").
    // [inlineReplyTarget] is the post the next send replies to; null
    // means the OP, the bar's historic target. A floor's Reply action
    // is routed here (focus the field, show the target chip); every
    // other post action (quote / comment / modify / report) keeps the
    // editor-sheet path via [onPostAction].
    var inlineReplyTarget by remember(route) { mutableStateOf<Post?>(null) }
    var inlineReplyText by remember(route) { mutableStateOf(TextFieldValue("")) }
    var inlineReplySending by remember(route) { mutableStateOf(false) }
    val inlineReplyFocus = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var inlineFocusEpoch by remember(route) { mutableStateOf(0) }
    LaunchedEffect(inlineFocusEpoch) {
        if (inlineFocusEpoch > 0) {
            runCatching { inlineReplyFocus.requestFocus() }
            keyboardController?.show()
        }
    }
    val focusInlineReply: (Post) -> Unit = { post ->
        inlineReplyTarget = post.takeIf { it.id != first?.id }
        inlineFocusEpoch++
    }
    val effectiveOnPostAction: ((action: String, post: Post) -> Unit)? =
        if (onPostAction == null) {
            null
        } else {
            { postAction, post ->
                if (postAction == PostRowAction.REPLY) {
                    focusInlineReply(post)
                } else {
                    onPostAction(postAction, post)
                }
            }
        }

    /** Send the bar's text as a reply to the current target. */
    fun sendInlineReply() {
        val ctrl = editor ?: return
        val targetPost = inlineReplyTarget ?: first ?: return
        val content = inlineReplyText.text
        if (content.isBlank() || inlineReplySending) return
        inlineReplySending = true
        ctrl.sendReplyInline(ctrl.replyTask(targetPost), content) { ok ->
            inlineReplySending = false
            if (ok) {
                inlineReplyText = TextFieldValue("")
                inlineReplyTarget = null
                keyboardController?.hide()
            }
        }
    }

    /** Hand the bar's text and target over to the full editor. */
    fun expandInlineReply() {
        val ctrl = editor ?: return
        val targetPost = inlineReplyTarget ?: first ?: return
        if (ctrl.postReply.showEditor.value) return
        val draft = inlineReplyText.text
        ctrl.replyWithDraft(targetPost, draft)
        // The editor owns the text now; clear the bar so the same
        // words cannot be sent twice.
        inlineReplyText = TextFieldValue("")
        inlineReplyTarget = null
        keyboardController?.hide()
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        containerColor = MiuixTheme.colorScheme.surface,
    ) { padding ->
        // 加载上一页：记录加载前的锚定楼层，加载完成后滚回该楼层——前面
        // 的 items 前插进来，但阅读位置保持不变。锚点优先取跳转目标
        // （jumpFloor，跳转/恢复场景），否则取屏幕顶部第一个可见楼层
        // （跳过 Header；Header 可见时也拿得到真实楼层）。
        var pendingAnchor by remember { mutableStateOf<Int?>(null) }
        // 目标页开头的楼层：跳过 0 楼（主题楼——主题在头部单独渲染，
        // 不在列表行里，滚动目标必须落在列表行上）。
        fun pageStartFloor(page: Int): Int? =
            dataSource.itemsAtPage(page).filter { it.floor > 0 }.minOfOrNull { it.floor }
        // scrollToPageStart=true（手势翻页）：加载完成后滚到目标页开头楼层
        // （ViewPager 翻页语义：翻页即显示新页开头，竖着滚动才连续阅读）；
        // 否则保持原阅读位置（滑到顶自动加载/跳转预载）。
        fun loadBack(
            prevPage: Int,
            jumpFloor: Int? = null,
            scrollToPageStart: Boolean = false,
            onDone: (() -> Unit)? = null,
        ) {
            val anchorFloor = if (scrollToPageStart) null else jumpFloor ?: currentRows
                .drop(listState.firstVisibleItemIndex)
                .firstOrNull { it is RowSpec.Reply }
                ?.let { (it as RowSpec.Reply).post.floor }
            dataSource.reload(page = prevPage, evenIfNotLoaded = true) {
                val target = if (scrollToPageStart) pageStartFloor(prevPage) else anchorFloor
                if (target != null) pendingAnchor = target
                onDone?.invoke()
            }
        }
        // reload 完成回调执行时重组尚未发生（currentRows 还是旧 rows），
        // 直接滚动会按旧 index 定位而错位；这里挂起等到 pendingAnchor
        // 变化后的重组完成，用最新 rows 计算 index，再瞬时回位——同帧
        // 完成，用户看到的位置不发生跳变。
        LaunchedEffect(pendingAnchor) {
            val floor = pendingAnchor ?: return@LaunchedEffect
            val index = rows.indexOfFirst { it.floor == floor }
            if (index >= 0) listState.scrollToItem(index)
            pendingAnchor = null
        }

        // ViewPager 式翻页动画：内容随手指横向平移，松手后按方向滑出
        // （左滑出左侧/右滑出右侧），加载完成后新内容从对侧滑入；未触发
        // 加载（位移不足/边界/加载中）则回弹。swipeLocked 期间手势只跟
        // 手不触发，避免动画与手势互相覆盖。
        val density = LocalDensity.current
        val screenWidthPx = with(density) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
        var contentOffset by remember { mutableFloatStateOf(0f) }
        var swipeLocked by remember { mutableStateOf(false) }
        var showSwipeOverlay by remember { mutableStateOf(false) }
        val swipeScope = rememberCoroutineScope()
        fun settleSwipe() {
            swipeScope.launch {
                swipeLocked = true
                animate(contentOffset, 0f, animationSpec = tween(220, easing = FastOutSlowInEasing)) { v, _ ->
                    contentOffset = v
                }
                swipeLocked = false
            }
        }
        // 按页码翻页（ViewPager 切换动画）：右滑 fromLeft=true（新页从左侧
        // 滑入）、左滑 fromLeft=false（从右侧滑入）。目标页已加载则直接滚到
        // 该页开头，不重复加载、不跨页跳转；未加载才发起加载（顺序页
        // loadMore，任意页 reload）。竖着滚动才是页内连续阅读（60→59）。
        fun swipeToPage(targetPage: Int, fromLeft: Boolean) {
            if (targetPage < 1) {
                settleSwipe()
                return
            }
            val exitX = if (fromLeft) screenWidthPx else -screenWidthPx
            swipeScope.launch {
                swipeLocked = true
                animate(
                    contentOffset, exitX,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                ) { v, _ -> contentOffset = v }
                if (dataSource.itemsAtPage(targetPage).isNotEmpty()) {
                    // 目标页已加载：直接滚到该页开头，无加载等待。
                    pageStartFloor(targetPage)?.let { pendingAnchor = it }
                    showSwipeOverlay = false
                    contentOffset = -exitX
                    animate(
                        contentOffset, 0f,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                    ) { v, _ -> contentOffset = v }
                    swipeLocked = false
                    return@launch
                }
                // 目标页未加载：发起加载。
                showSwipeOverlay = true
                var ok = false
                if (fromLeft) {
                    // 上一页：reload 任意页。
                    loadBack(targetPage, scrollToPageStart = true) { ok = true }
                    while (!ok && dataSource.isLoading) delay(50)
                } else {
                    // 下一页：顺序页走 loadMore；中间缺页（页号 ≤ 已加载页数但
                    // 内容缺失）走 reload；超出末尾则不加载。
                    when {
                        dataSource.hasMore && targetPage == dataSource.loadedPage + 1 -> {
                            val before = dataSource.items.size
                            dataSource.loadMore()
                            while (dataSource.isLoading) delay(50)
                            ok = dataSource.items.size != before &&
                                dataSource.state.value.latestError == null
                        }
                        targetPage <= dataSource.loadedPage -> {
                            loadBack(targetPage, scrollToPageStart = true) { ok = true }
                            while (!ok && dataSource.isLoading) delay(50)
                        }
                        else -> ok = false
                    }
                }
                showSwipeOverlay = false
                if (ok) {
                    pageStartFloor(targetPage)?.let { pendingAnchor = it }
                    contentOffset = -exitX
                    animate(
                        contentOffset, 0f,
                        animationSpec = tween(240, easing = FastOutSlowInEasing),
                    ) { v, _ -> contentOffset = v }
                    swipeLocked = false
                } else {
                    settleSwipe()
                }
            }
        }

        // 滑到当前页顶部时自动加载上一页（无需下拉手势）；加载中由列表
        // 顶部的"正在加载上一页"提示展示。只有第一页顶部下拉才是刷新。
        // 注意：snapshotFlow 只追踪列表位置（Compose snapshot 状态），
        // 数据源状态在 collect 时实时读取。
        val isLoadingPrev =
            state.isLoading && (dataSource.firstLoadedPage ?: 1) > 1
        fun maybeAutoLoadPrev() {
            if (listState.firstVisibleItemIndex <= 1 &&
                (dataSource.firstLoadedPage ?: 1) > 1 &&
                !dataSource.state.value.isLoading
            ) {
                loadBack((dataSource.firstLoadedPage ?: 1) - 1)
            }
        }
        // 1) 用户滚动到顶部时触发。
        LaunchedEffect(dataSource, listState) {
            snapshotFlow { listState.firstVisibleItemIndex <= 1 }
                .collect { atTop -> if (atTop) maybeAutoLoadPrev() }
        }
        // 2) 跳转/刷新后兜底：自动预载上一页一次（保证跳转到后面楼层后能
        //    往回滑；内容不满一屏无法滚动触发时也不会卡住）。reload 不更新
        //    lastRefreshTime，所以不会连发。锚定跳转目标楼层（jumpFloor 的
        //    快照——跳转滚动 effect 稍后会把该变量清空，需先捕获）。
        LaunchedEffect(state.lastRefreshTime) {
            if (state.lastRefreshTime != null && (dataSource.firstLoadedPage ?: 1) > 1) {
                val jumpFloor = floorToJump
                delay(300)
                // 等待当前加载（如跳转后触发的 loadMore）结束，否则
                // reload 的 isLoading guard 会拒绝预载。
                while (dataSource.state.value.isLoading) {
                    delay(100)
                }
                loadBack((dataSource.firstLoadedPage ?: 1) - 1, jumpFloor)
            }
        }

        // 第一页下拉刷新；>第一页下拉 = 手动加载上一页（滑到顶自动加载
        // 失败时的兜底入口），无翻页动画、保持位置，与自动触发一致（顶部
        // 显示"正在加载上一页"提示）。
        val pullRefreshState = rememberPullToRefreshState()
        Box(Modifier.fillMaxSize().padding(padding)) {
        Column(Modifier.fillMaxSize()) {
            // Large-title header (v1.8.0, per the confirmed mock): back
            // chevron, the forum name big with the mode/floor-count line
            // under it, and round star / overflow buttons. The subject
            // itself is the in-content TopicSubjectHeader, as in the
            // mock — the old SmallTopAppBar duplicated it as its title.
            Row(
                Modifier.fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = { navigator.pop() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                }
                // v1.8.4: the title area doubles as a jump entry —
                // tapping the forum name / floor-count line opens the
                // jump selector (the ⋮ menu carries the same action).
                Column(
                    Modifier.weight(1f).then(
                        if (!mock && onlyPostId == null) {
                            Modifier.clickable { showJumpSelector = true }
                        } else {
                            Modifier
                        },
                    ),
                ) {
                    Text(
                        atForum?.name
                            ?: topic.parentForum.takeIf { topic.hasParentForum() }?.name
                            ?: "",
                        style = MiuixTheme.textStyles.title2,
                        fontWeight = FontWeight.Bold,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                    val subLabel = when {
                        localMode -> L.str(context, "Cached Topic")
                        !enableAuthorOnly && authorOnlyMode ->
                            L.str(context, "Author Only")
                        onlyPostId != null -> L.str(context, "Reply")
                        maxFloor > 0 -> "共 ${maxFloor + 1} 楼"
                        else -> null
                    }
                    if (subLabel != null) {
                        Text(
                            subLabel,
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            maxLines = 1,
                        )
                    }
                }
                if (state.isLoading) {
                    CircularProgressIndicator(size = 20.dp, strokeWidth = 2.dp)
                    Spacer(Modifier.size(12.dp))
                }
                // v1.8.4: the header bell (notifications entry) is gone
                // per the user's call — notifications live on the home
                // header / personal center, not inside a topic.
                if (!mock) {
                    // The mock's star button: the same favorite toggle
                    // the More menu carries.
                    // v1.8.4 glass sweep: the header round buttons are
                    // glass now, sampling this screen's (flat, by the
                    // crash-isolation rule) backdrop like the reply bar.
                    Box(
                        Modifier.size(40.dp)
                            .liquidGlass(
                                backdrop = indicatorBackdrop,
                                shape = androidx.compose.foundation.shape.CircleShape,
                                spec = glass.spec,
                                scheme = glass.scheme,
                                shadowRadius = 8.dp,
                                debugShowSample = glass.debugShowSample,
                                debugColor = Color.Cyan,
                            )
                            .clip(androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        IconButton(
                            onClick = {
                                toggleTopicFavor(
                                    scope = scope,
                                    view = view,
                                    topicId = topic.id,
                                    currentFavored = favoredOverride ?: topic.isFavored,
                                    onResult = { favored -> favoredOverride = favored },
                                )
                            },
                        ) {
                            Icon(
                                if (favoredOverride ?: topic.isFavored) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                                contentDescription = null,
                                tint = MiuixTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Spacer(Modifier.size(10.dp))
                }
                Box(
                    Modifier.size(40.dp)
                        .liquidGlass(
                            backdrop = indicatorBackdrop,
                            shape = androidx.compose.foundation.shape.CircleShape,
                            spec = glass.spec,
                            scheme = glass.scheme,
                            shadowRadius = 8.dp,
                            debugShowSample = glass.debugShowSample,
                            debugColor = Color.Cyan,
                        )
                        .clip(androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    TopicDetailsMoreMenu(
                        topic = topic,
                        enableAuthorOnly = enableAuthorOnly,
                        localMode = localMode,
                        atForum = atForum,
                        isFavorite = favoredOverride ?: topic.isFavored,
                        onToggleFavorite = if (!mock) {
                            {
                                toggleTopicFavor(
                                    scope = scope,
                                    view = view,
                                    topicId = topic.id,
                                    currentFavored = favoredOverride ?: topic.isFavored,
                                    onResult = { favored -> favoredOverride = favored },
                                )
                            }
                        } else {
                            null
                        },
                        onAuthorOnly = {
                            if (PlusModel.checkPlus(PlusFeature.AUTHOR_ONLY)) {
                                val isAnonymous = topic.authorName.anonymous.isNotEmpty()
                                action.navigateToAuthorOnly.value =
                                    if (isAnonymous) {
                                        TopicDetailsActionModel.AuthorOnly.Anonymous(null)
                                    } else {
                                        TopicDetailsActionModel.AuthorOnly.ByUID(topic.authorId)
                                    }
                            }
                        },
                        onViewCached = { action.navigateToLocalMode.value = true },
                        onGotoForum = { forum -> action.navigateToForum.value = forum },
                        // v1.8.4: no silent gate here anymore. The
                        // entry used to wrap the open in
                        // PlusModel.checkPlus(JUMP) — a silent
                        // early-return with zero feedback, the same
                        // pattern that made the topic list's pencil
                        // button read as "clicks do nothing" — and the
                        // only other entry was the page capsule, which
                        // is deleted. Jump now opens directly, from
                        // this menu and from the header title area.
                        onJump = if (!mock && onlyPostId == null) {
                            { showJumpSelector = true }
                        } else null,
                        onLoadFirstPage = run {
                            val firstLoaded = dataSource.firstLoadedPage
                            if (firstLoaded != null && firstLoaded >= 2) {
                                {
                                    dataSource.loadFromPage = 1
                                    floorToJump = 0
                                }
                            } else null
                        },
                        onReply = if (onlyPostId == null && !mock && first != null) {
                            { focusInlineReply(first) }
                        } else null,
                        onGotoTopic = if (onlyPostId != null && topic.id.isNotEmpty()) {
                            {
                                // Leave the single-post view for the full topic.
                                navigator.push(
                                    Route.TopicDetails(
                                        topicId = topic.id,
                                        fav = route.fav,
                                    )
                                )
                            }
                        } else null,
                    )
                }
            }
        PullToRefresh(
            isRefreshing = state.isRefreshing,
            onRefresh = {
                if ((dataSource.firstLoadedPage ?: 1) <= 1) {
                    dataSource.refreshAsync(sleepMillis = 500)
                } else if (!dataSource.isLoading) {
                    loadBack((dataSource.firstLoadedPage ?: 1) - 1)
                }
            },
            pullToRefreshState = pullRefreshState,
            modifier = Modifier.weight(1f),
        ) {
            Box(
                Modifier.fillMaxSize().lowResLayerBackdrop(indicatorBackdrop)
                    .recordedPageBackground(),
            ) {
            // v1.8.5-exp: THIS content box is the recording node (see
            // the backdrop note at rememberGlassSettings above): the
            // post content itself is recorded into indicatorBackdrop
            // — the v1.8.0/v1.8.2 geometry — but at
            // DETAILS_BACKDROP_SCALE linear resolution via
            // LowResLayerBackdrop (~1/16 the pixels of the full-res
            // recordings that crashed heavy topics in v1.8.1/v1.8.2).
            // This replaces the v1.8.1/v1.8.3 flat page-background
            // node that sat here recording only the surface color.
            // The background underlay stays in the chain, inward of
            // the recording node, so undrawn regions of the texture
            // hold the page color (the v1.7.8 rule) and consumer
            // coordinates are unchanged (same bounds as before).
            if (localMode) {
                LocalCacheBanner(
                    reason = response?.localReason ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            if (response == null && state.latestError?.isXMLParseError == true) {
                XMLParseErrorMain(
                    topic = topic,
                    onRefresh = { dataSource.refresh(animated = true) },
                    onOpenInBrowser = { openInBrowser(context, topic) },
                )
            } else {
                // While the first page loads, the toolbar spinner is the only
                // loading indicator; render the (empty) list below it.
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        // ViewPager 式横向翻页：右滑加载上一页、左滑加载下一页。
                        // 拖动时内容跟手平移；松手时累计位移 ≥70dp 且边界允许则
                        // 滑出→加载→新内容从对侧滑入，否则回弹。手势期间列表
                        // 垂直滚动让位于水平手势；系统边缘返回手势仍由系统处理。
                        .pointerInput(dataSource) {
                            val threshold = with(density) { 70.dp.toPx() }
                            var totalX = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { totalX = 0f },
                                onDragEnd = {
                                    val dx = totalX
                                    totalX = 0f
                                    if (swipeLocked) return@detectHorizontalDragGestures
                                    if (kotlin.math.abs(dx) < threshold) {
                                        settleSwipe()
                                        return@detectHorizontalDragGestures
                                    }
                                    // 当前页：屏幕顶部第一个可见楼层所在页；无可见
                                    // 楼层（顶部 Header 区）时取最早加载页。
                                    val curFloor = currentRows
                                        .drop(listState.firstVisibleItemIndex)
                                        .firstOrNull { it is RowSpec.Reply }
                                        ?.let { (it as RowSpec.Reply).post.floor }
                                    val curPage = curFloor?.let { f ->
                                        dataSource.pagedItems()
                                            .firstOrNull { (_, its) -> its.any { it.floor == f } }
                                            ?.first
                                    } ?: (dataSource.firstLoadedPage ?: 1)
                                    // 按页码翻页：右滑到上一页（从左侧滑入），
                                    // 左滑到下一页（从右侧滑入）。
                                    if (dx > 0) swipeToPage(curPage - 1, fromLeft = true)
                                    else swipeToPage(curPage + 1, fromLeft = false)
                                },
                                onDragCancel = {
                                    totalX = 0f
                                    settleSwipe()
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                totalX += dragAmount
                                if (!swipeLocked) contentOffset = totalX
                            }
                        }
                        // 跟手平移发生在 graphicsLayer：绘制/命中随平移变化，
                        // 手势判定仍用原始坐标。
                        .graphicsLayer { translationX = contentOffset },
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        // Reply bar slot: 100.dp for the single-row bar;
                        // the target chip row adds its height on top.
                        bottom = if (showReplyBar) {
                            if (inlineReplyTarget != null) 136.dp else 100.dp
                        } else {
                            8.dp
                        },
                    ),
                ) {
                    // 自动加载上一页时，顶部居中显示加载提示。
                    if (isLoadingPrev) {
                        item(key = "loading-prev") {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 14.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        size = 16.dp,
                                        strokeWidth = 2.dp,
                                    )
                                    Text(
                                        L.str(context, "Loading Previous Page"),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                    )
                                }
                            }
                        }
                    }
                    itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                        // Grouped card (v1.8.0 MIUIX layout): every row
                        // between the subject header and the tail — the
                        // OP, the hot band, the numbered floors — shares
                        // one large-corner card surface; the header and
                        // the tail stay outside it (per the mock). The
                        // hot band paints its heat wash over the card
                        // surface within its own bounds.
                        val inCard = row !is RowSpec.Header && row !is RowSpec.Tail
                        val prevInCard = rows.getOrNull(index - 1)?.let {
                            it !is RowSpec.Header && it !is RowSpec.Tail
                        } == true
                        val nextInCard = rows.getOrNull(index + 1)?.let {
                            it !is RowSpec.Header && it !is RowSpec.Tail
                        } == true
                        val cardShape = when {
                            !inCard -> RoundedCornerShape(0.dp)
                            !prevInCard && !nextInCard -> RoundedCornerShape(24.dp)
                            !prevInCard -> RoundedCornerShape(
                                topStart = 24.dp, topEnd = 24.dp,
                            )
                            !nextInCard -> RoundedCornerShape(
                                bottomStart = 24.dp, bottomEnd = 24.dp,
                            )
                            else -> RoundedCornerShape(0.dp)
                        }
                        Column(
                            if (inCard) {
                                Modifier.fillMaxWidth().clip(cardShape)
                                    .background(MiuixTheme.colorScheme.surfaceContainer)
                            } else {
                                Modifier
                            },
                        ) {
                            TopicDetailsRow(
                                row = row,
                                index = index,
                                isLoading = state.isLoading,
                                topic = topic,
                                action = action,
                                votes = votes,
                                quotedPosts = quotedPosts,
                                viewingImage = viewingImage,
                                enableAuthorOnly = enableAuthorOnly,
                                scrollToPid = scrollToPid,
                                locateFloor = childLocateFloor,
                                dataSource = dataSource,
                                currentViewingFloor = currentViewingFloor,
                                onPostAction = effectiveOnPostAction,
                                onLoadNewReplies = {
                                    dataSource.reloadLastPage(evenIfNotLoaded = true) {
                                        Haptics.vibrate(context, Haptics.NotificationType.SUCCESS)
                                    }
                                },
                            )
                            // 楼层之间用卡内发丝线分隔（v1.8.0 分组卡）。
                            // 热点回复区自带热色底纹作为边界，共用分割线不再切进去。
                            val bordersHotBand = row.paintsOwnEdges ||
                                rows.getOrNull(index + 1)?.paintsOwnEdges == true
                            if (inCard && nextInCard && !bordersHotBand) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 16.dp),
                                    thickness = 0.5.dp,
                                    color = MiuixTheme.colorScheme.dividerLine,
                                )
                            }
                        }
                    }
                }
                // 翻页切换中（内容已滑出屏幕、等待新页加载）的居中加载指示。
                if (showSwipeOverlay) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(MiuixTheme.colorScheme.surface.copy(alpha = 0.72f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            // v1.8.4: the page-indicator capsule that floated here is
            // deleted (component, stacking offsets and its settings
            // toggle all gone). Nothing floats at the bottom of this
            // screen except the inline reply bar below.
            }
        }
        }
        if (showReplyBar) {
            // v1.8.4 IME follow: up to v1.8.3 the bar only carried
            // navigationBarsPadding, so when the keyboard opened the
            // bar stayed sunk at the very bottom, hidden behind the
            // IME it had just summoned. The bottom inset is now
            // max(ime, navigationBars): with the keyboard up the bar
            // parks on the keyboard's top edge, with it down it falls
            // back to exactly the old slot (nav inset + 12.dp). The
            // ime inset is read in composition, so it tracks the
            // system's IME animation frame by frame instead of
            // jumping. (The list's bottom content padding is sized
            // for the keyboard-down slot and deliberately unchanged:
            // while typing, the bar floats over the list tail like
            // every other messenger's input bar.)
            val imeDensity = LocalDensity.current
            val imeBottomPx = WindowInsets.ime.getBottom(imeDensity)
            val navBottomPx = WindowInsets.navigationBars.getBottom(imeDensity)
            val replyBarBottom = with(imeDensity) {
                maxOf(imeBottomPx, navBottomPx).toDp()
            } + 12.dp
            InlineReplyBar(
                backdrop = indicatorBackdrop,
                spec = glass.spec,
                scheme = glass.scheme,
                debugShowSample = glass.debugShowSample,
                target = inlineReplyTarget,
                text = inlineReplyText,
                onTextChange = { inlineReplyText = it },
                focusRequester = inlineReplyFocus,
                sending = inlineReplySending,
                onSend = { sendInlineReply() },
                onExpand = { expandInlineReply() },
                onClearTarget = { inlineReplyTarget = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = replyBarBottom),
            )
        }
        }
    }

    // Overlays.
    if (showJumpSelector) {
        TopicJumpSelector(
            maxFloor = maxFloor,
            mode = jumpSelectorMode,
            initialFloor = currentViewingFloor.currentLowest ?: 0,
            onModeChange = { jumpSelectorMode = it },
            onDismiss = { showJumpSelector = false },
            onJump = { floor, page ->
                floorToJump = floor
                dataSource.loadFromPage = page
            },
        )
    }
    (replyChain ?: quotedReplies)?.let { chain ->
        ReplyChainOverlay(
            chain = chain,
            topic = topic,
            resolver = quotedPosts,
            votes = votes,
            viewingImage = viewingImage,
            onDismiss = {
                action.showingReplyChain.value = null
                action.showingQuotedReplies.value = null
            },
            onNavigateAuthorOnly = { post ->
                val isAnonymous = App.users.localUser(post.authorId)
                    ?.name?.anonymous?.isNotEmpty() == true
                action.navigateToAuthorOnly.value =
                    if (isAnonymous) {
                        TopicDetailsActionModel.AuthorOnly.Anonymous(post.id)
                    } else {
                        TopicDetailsActionModel.AuthorOnly.ByUID(post.authorId)
                    }
            },
        )
    }
    ImageViewerDialog(model = viewingImage)
}

/**
 * The floating reply bar of the topic screen (v1.8.0, per the confirmed
 * mock): a liquid-glass capsule in the shared glass recipe (same
 * resolution and three-material setting as the dock), sampling the
 * screen's own backdrop from OUTSIDE the recorded subtree.
 *
 * v1.8.3: the bar is a real input now. Up to v1.8.2 the whole bar was
 * a tap target opening the editor sheet (a SuperBottomSheet in its
 * own Dialog window), and on device the tap animated but text could
 * never be entered — two rounds of focus/IME hardening inside that
 * sheet did not change it. The field below lives in THIS window:
 * tapping it focuses and raises the IME directly, the 发送 pill posts
 * through PostReplyModel.sendDirect (the editor's own requests), and
 * the expand button hands the typed text to the full editor for the
 * flows that need it (attachments, stickers, …). When a floor's Reply
 * action targeted another post, a chip row names that target and can
 * clear it back to the OP.
 */
@Composable
private fun InlineReplyBar(
    backdrop: Backdrop,
    spec: GlassSpec,
    scheme: DockScheme,
    debugShowSample: Boolean,
    target: Post?,
    text: TextFieldValue,
    onTextChange: (TextFieldValue) -> Unit,
    focusRequester: FocusRequester,
    sending: Boolean,
    onSend: () -> Unit,
    onExpand: () -> Unit,
    onClearTarget: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val shape = if (target != null) {
        RoundedCornerShape(26.dp)
    } else {
        RoundedCornerShape(percent = 50)
    }
    Column(
        modifier
            .fillMaxWidth()
            .liquidGlass(
                backdrop = backdrop,
                shape = shape,
                spec = spec,
                scheme = scheme,
                shadowRadius = 12.dp,
                debugShowSample = debugShowSample,
                debugColor = Color.Cyan,
            )
            .clip(shape)
            .padding(start = 16.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
    ) {
        if (target != null) {
            val authorName = App.users.cachedUser(target.authorId)
                ?.name?.display()?.takeIf { it.isNotEmpty() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (authorName != null) {
                        "回复 #${target.floor} 楼 · $authorName"
                    } else {
                        "回复 #${target.floor} 楼"
                    },
                    style = MiuixTheme.textStyles.footnote1,
                    color = glassContentColor(scheme),
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "取消回复目标",
                    tint = glassContentColor(scheme),
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(percent = 50))
                        .clickable(onClick = onClearTarget)
                        .padding(5.dp),
                )
            }
        }
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.weight(1f).padding(start = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (text.text.isEmpty()) {
                    Text(
                        if (target != null) "回复这一楼…" else "回复楼主…",
                        style = MiuixTheme.textStyles.body1,
                        color = glassContentColor(scheme).copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
                BasicTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = MiuixTheme.textStyles.body1.copy(
                        color = glassContentColor(scheme),
                    ),
                    cursorBrush = SolidColor(MiuixTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    maxLines = 4,
                )
            }
            IconButton(onClick = onExpand) {
                Icon(
                    Icons.Outlined.OpenInFull,
                    contentDescription = L.str(context, "Open Editor"),
                    tint = glassContentColor(scheme),
                    modifier = Modifier.size(20.dp),
                )
            }
            val canSend = text.text.isNotBlank() && !sending
            Box(
                Modifier
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        if (canSend) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.primary.copy(alpha = 0.45f)
                        }
                    )
                    .clickable(enabled = canSend, onClick = onSend)
                    .padding(horizontal = 22.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (sending) "发送中…" else "发送",
                    style = MiuixTheme.textStyles.body1,
                    fontWeight = FontWeight.SemiBold,
                    color = MiuixTheme.colorScheme.onPrimary,
                )
            }
        }
    }
}

// region Supporting models

/** Queue an author-only view of the topic for the author of [post]. */
private fun navigateAuthorOnly(action: TopicDetailsActionModel, post: Post) {
    val isAnonymous = App.users.localUser(post.authorId)
        ?.name?.anonymous?.isNotEmpty() == true
    action.navigateToAuthorOnly.value =
        if (isAnonymous) {
            TopicDetailsActionModel.AuthorOnly.Anonymous(post.id)
        } else {
            TopicDetailsActionModel.AuthorOnly.ByUID(post.authorId)
        }
}

/** Tracks the floors currently visible on screen. */
class CurrentViewingFloor {
    private val floors = mutableSetOf<Int>()
    var highestSeen: Int? = null
        private set

    val currentLowest: Int? get() = floors.minOrNull()

    fun appear(floor: Int) {
        floors.add(floor)
    }

    fun disappear(floor: Int) {
        floors.remove(floor)
        highestSeen = maxOf(highestSeen ?: 0, floor)
    }
}

/** One LazyColumn row descriptor. */
sealed interface RowSpec {
    val key: String
    val floor: Int? get() = null
    val pid: String? get() = null

    /**
     * True for the rows of the hot-reply band, which paint their own edges —
     * the stream's shared rule must not cut into the block, from either side.
     */
    val paintsOwnEdges: Boolean get() = false

    data object Header : RowSpec {
        override val key: String = "header"
    }

    /**
     * The main (OP) post, split out of [Header] in v1.8.0 so the
     * grouped card starts here — the subject header stays above the
     * card, per the confirmed mock.
     */
    data class MainPost(val post: Post) : RowSpec {
        override val key: String = "main_post"
        override val floor: Int get() = post.floor
        override val pid: String get() = post.id.pid
    }

    /** Opens the hot-reply band: the flame marker over [count] replies. */
    data class HotHeader(val count: Int) : RowSpec {
        override val key: String = "hot_header"
        override val paintsOwnEdges: Boolean = true
    }

    /**
     * A hot reply, [index] of [count] — its place in the band, so the row can
     * paint its slice of the one rail that fades down the whole block.
     */
    data class HotReply(val post: Post, val index: Int, val count: Int) : RowSpec {
        override val key: String = "hot_${post.id.pid}"
        override val pid: String get() = post.id.pid
        override val paintsOwnEdges: Boolean = true

        val isLast: Boolean get() = index == count - 1

        /** The rail's opacity where this row starts, then where it ends. */
        val railFrom: Float get() = 1f - index.toFloat() / count
        val railTo: Float get() = 1f - (index + 1).toFloat() / count
    }

    data class Reply(val post: Post) : RowSpec {
        override val key: String = "floor_${post.floor}"
        override val floor: Int get() = post.floor
        override val pid: String get() = post.id.pid
    }

    data object Tail : RowSpec {
        override val key: String = "tail"
    }
}

private fun buildRows(
    items: List<Post>,
    first: Post?,
    showTail: Boolean,
): List<RowSpec> {
    val rows = mutableListOf<RowSpec>(RowSpec.Header)
    if (first != null) rows += RowSpec.MainPost(first)
    first?.hotRepliesList?.takeIf { it.isNotEmpty() }?.let { hot ->
        rows += RowSpec.HotHeader(hot.size)
        hot.forEachIndexed { index, post ->
            rows += RowSpec.HotReply(post, index = index, count = hot.size)
        }
    }

    val paged = run {
        val byPage = items.groupBy { it.atPage }
        byPage.keys.sorted().map { page -> page to byPage.getValue(page).sortedBy { it.floor } }
    }
    for ((page, posts) in paged) {
        for (post in posts) {
            if (first != null && post.id == first.id) continue
            rows += RowSpec.Reply(post)
        }
    }
    if (showTail) rows += RowSpec.Tail
    return rows
}

private fun shouldShowTailSection(
    dataSource: com.bugenzhao.mnga.model.PagingDataSource<TopicDetailsResponse, Post>,
    state: com.bugenzhao.mnga.model.PagingDataSource.State<Post>,
    response: TopicDetailsResponse?,
    onlyPost: Boolean,
): Boolean {
    if (onlyPost || response == null) return false
    if (dataSource.isInitialLoading) return false
    if (dataSource.hasMore) return false
    val lastRefresh = state.lastRefreshTime ?: return false
    val lastReply = state.items.maxOfOrNull { it.postDate } ?: return false
    if (lastReply <= 0) return false
    return Date(lastReply * 1000).time >= lastRefresh.time - 3600_000
}

/** Builds the `topicDetails`-backed data source per the RPC table. */
internal fun buildTopicDetailsDataSource(
    scope: kotlinx.coroutines.CoroutineScope,
    topicId: String,
    fav: String?,
    onlyPostId: String?,
    localCache: Boolean,
    authorId: String?,
    anonymousAuthorOnly: Boolean,
    useDisabledStrategy: Boolean,
    finishOnError: Boolean,
    initialPage: Int,
): com.bugenzhao.mnga.model.PagingDataSource<TopicDetailsResponse, Post> =
    com.bugenzhao.mnga.model.PagingDataSource(
        scope = scope,
        responseParser = { TopicDetailsResponse.parser() },
        buildRequest = { page ->
            val builder = TopicDetailsRequest.newBuilder().setTopicId(topicId).setPage(page)
            if (useDisabledStrategy) {
                builder.setWebApiStrategyValue(0) // DISABLED
            } else {
                builder.setWebApiStrategyValue(App.prefs.topicDetailsWebApiStrategy.raw)
            }
            fav?.let { builder.fav = it }
            onlyPostId?.let { builder.setPostId(it) }
            if (localCache) builder.setLocalCache(true)
            authorId?.let { builder.setAuthorId(it) }
            if (anonymousAuthorOnly) {
                builder.setAnonymousAuthorOnly(true)
            }
            AsyncRequest.newBuilder().setTopicDetails(builder).build()
        },
        onResponse = { resp ->
            if (resp.hasLocalReason()) {
                ToastModel.showAuto(ToastModel.Message.CacheLoaded(resp.localReason))
            }
            resp.inPlaceUsersList.forEach { App.users.add(it) }
            Pair(resp.repliesList, resp.pages.toInt().takeIf { it > 0 })
        },
        id = { it.floor.toString() },
        finishOnError = finishOnError,
        initialPage = initialPage,
    )

/** Fire-and-forget reading-progress sync, run off the main thread. */
private fun syncTopicProgress(topic: Topic, viewing: CurrentViewingFloor) {
    if (topic.id.isEmpty() || topic.id.startsWith("mnga_")) return
    val seen = viewing.highestSeen ?: 0
    val highest = maxOf(if (topic.hasHighestViewedFloor()) topic.highestViewedFloor else 0, seen)
    val current = viewing.currentLowest ?: 0
    appScope.launch {
        withContext(Dispatchers.IO) {
            runCatching {
                logicCall(
                    SyncRequest.newBuilder()
                        .setUpdateTopicProgress(
                            UpdateTopicProgressRequest.newBuilder()
                                .setTopicId(topic.id)
                                .setHighestFloor(highest)
                                .setCurrentFloor(current)
                        )
                        .build()
                )
            }
        }
    }
}

private fun openInBrowser(context: android.content.Context, topic: Topic) {
    val navID = NavigationIdentifier.TopicID(topic.id, topic.fav.takeIf { it.isNotEmpty() })
    navID.webpageURL?.let {
        runCatching {
            context.startActivity(android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(it)))
        }
    }
}

// endregion

// region Row rendering

@Composable
private fun TopicDetailsRow(
    row: RowSpec,
    index: Int,
    isLoading: Boolean,
    topic: Topic,
    action: TopicDetailsActionModel,
    votes: VotesModel,
    quotedPosts: QuotedPostResolver,
    viewingImage: ViewingImageModel,
    enableAuthorOnly: Boolean,
    scrollToPid: String?,
    locateFloor: ((Post) -> Unit)?,
    dataSource: com.bugenzhao.mnga.model.PagingDataSource<TopicDetailsResponse, Post>,
    currentViewingFloor: CurrentViewingFloor,
    onPostAction: ((action: String, post: Post) -> Unit)?,
    onLoadNewReplies: () -> Unit,
) {
    val context = LocalContext.current

    when (row) {
        RowSpec.Header -> {
            // The subject header alone: the OP moved to its own row
            // (v1.8.0) so the grouped card starts at the OP, below the
            // title — the full-width divider that used to separate
            // header and stream goes with the old flat layout.
            TopicSubjectHeader(topic = topic)
        }

        is RowSpec.MainPost -> {
            PostRow(
                post = row.post,
                isAuthor = row.post.authorId == topic.authorId,
                action = action,
                votes = votes,
                quotedResolver = quotedPosts,
                viewingImage = viewingImage,
                enableAuthorOnly = enableAuthorOnly,
                onPostAction = onPostAction,
                onNavigateAuthorOnly = { navigateAuthorOnly(action, it) },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        is RowSpec.HotHeader -> HotBandHeader(count = row.count)

        is RowSpec.HotReply -> Box(
            Modifier.hotBand(
                railFrom = row.railFrom,
                railTo = row.railTo,
                separated = !row.isLast,
            ),
        ) {
            PostRow(
                post = row.post,
                isAuthor = row.post.authorId == topic.authorId,
                action = action,
                votes = votes,
                quotedResolver = quotedPosts,
                viewingImage = viewingImage,
                enableAuthorOnly = enableAuthorOnly,
                onPostAction = onPostAction,
                onNavigateAuthorOnly = { navigateAuthorOnly(action, it) },
                modifier = Modifier.padding(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = if (row.isLast) 14.dp else 12.dp,
                ),
            )
        }

        is RowSpec.Reply -> ReplyRow(
            post = row.post,
            index = index,
            topic = topic,
            action = action,
            votes = votes,
            quotedPosts = quotedPosts,
            viewingImage = viewingImage,
            enableAuthorOnly = enableAuthorOnly,
            scrollToPid = scrollToPid,
            locateFloor = locateFloor,
            dataSource = dataSource,
            currentViewingFloor = currentViewingFloor,
            onPostAction = onPostAction,
        )

        RowSpec.Tail -> {
            val loading = isLoading
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                    Button(onClick = { if (!loading) onLoadNewReplies() }, enabled = !loading) {
                        Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(L.str(context, "Load New Replies"))
                    }
                    Spacer(Modifier.weight(1f))
                    if (loading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
        }
    }
}

// region Hot-reply band

/** Width of the ember rail down the hot band's left edge. */
private val HotRailWidth = 3.dp

/**
 * The warm band a hot-reply row sits on: a soft heat wash over the otherwise
 * white stream, an inset hairline between rows, and — down the left edge — this
 * row's slice of a single rail that burns opaque at the top of the block and
 * fades to nothing by its end, the way the replies themselves cool off as they
 * go. [railFrom] and [railTo] are that rail's opacity where the row starts and
 * where it ends; row *n*'s [railTo] is row *n+1*'s [railFrom], so the segments
 * join without a seam. The wash lifting is the block's only closing edge —
 * nothing is ruled off against the numbered floors that follow.
 */
@Composable
private fun Modifier.hotBand(
    railFrom: Float = 1f,
    railTo: Float = 1f,
    separated: Boolean = false,
): Modifier {
    val heat = hotAccentColor()
    val dark = MiuixTheme.colorScheme.surface.luminance() < 0.5f
    // Dark surfaces swallow a tint, so the wash there is twice as strong.
    val wash = heat.copy(alpha = if (dark) 0.10f else 0.05f)
    return this
        .fillMaxWidth()
        .drawBehind {
            drawRect(wash)

            // Both stops are a *heat* — the tail is transparent orange, never
            // transparent black, which the unpremultiplied blend would gray.
            val rail = HotRailWidth.toPx()
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(heat.copy(alpha = railFrom), heat.copy(alpha = railTo)),
                    startY = 0f,
                    endY = size.height,
                ),
                size = Size(rail, size.height),
            )

            if (separated) {
                val inset = 16.dp.toPx()
                drawLine(
                    color = heat.copy(alpha = 0.2f),
                    start = Offset(inset, size.height),
                    end = Offset(size.width - inset, size.height),
                    strokeWidth = 1f,
                )
            }
        }
}

/**
 * Opens the hot-reply band: a flame, the label, how many replies made the cut,
 * and a hairline that fades out to the right the way an editorial rule does.
 */
@Composable
private fun HotBandHeader(count: Int) {
    val context = LocalContext.current
    val heat = hotAccentColor()
    Row(
        Modifier
            .hotBand()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.LocalFireDepartment,
            contentDescription = null,
            modifier = Modifier.size(15.dp),
            tint = heat,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            L.str(context, "Hot Replies"),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = heat,
        )
        if (count > 1) {
            Spacer(Modifier.width(6.dp))
            Text(
                count.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = heat.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(heat.copy(alpha = 0.3f), heat.copy(alpha = 0f)),
                    )
                ),
        )
    }
}

// endregion

@Composable
private fun ReplyRow(
    post: Post,
    index: Int,
    topic: Topic,
    action: TopicDetailsActionModel,
    votes: VotesModel,
    quotedPosts: QuotedPostResolver,
    viewingImage: ViewingImageModel,
    enableAuthorOnly: Boolean,
    scrollToPid: String?,
    locateFloor: ((Post) -> Unit)?,
    dataSource: com.bugenzhao.mnga.model.PagingDataSource<TopicDetailsResponse, Post>,
    currentViewingFloor: CurrentViewingFloor,
    onPostAction: ((action: String, post: Post) -> Unit)?,
) {
    // Track the viewing floor while this row is composed.
    DisposableEffect(post.id) {
        currentViewingFloor.appear(post.floor)
        onDispose { currentViewingFloor.disappear(post.floor) }
    }
    // Prefetch near the end of the list.
    DisposableEffect(post.id, index) {
        dataSource.loadMoreIfNeeded(index)
        onDispose { }
    }

    PostRow(
        post = post,
        isAuthor = post.authorId == topic.authorId,
        action = action,
        votes = votes,
        quotedResolver = quotedPosts,
        viewingImage = viewingImage,
        enableAuthorOnly = enableAuthorOnly,
        locateFloor = locateFloor,
        shouldHighlight = scrollToPid == post.id.pid,
        onHighlightConsumed = { action.scrollToPid.value = null },
        onPostAction = onPostAction,
        onNavigateAuthorOnly = { navigateAuthorOnly(action, it) },
        modifier = Modifier.padding(16.dp),
    )
}

/** Topic subject with optional tag bar. */
/** 话题色池：浅底 + 深字/描边；同一话题按 hash 稳定取色。 */
private val TagChipPalette = listOf(
    Color(0xFFE8F0FE) to Color(0xFF1A73E8), // 蓝
    Color(0xFFFCE8E6) to Color(0xFFD93025), // 红
    Color(0xFFE6F4EA) to Color(0xFF188038), // 绿
    Color(0xFFFEF7E0) to Color(0xFFB06000), // 琥珀
    Color(0xFFF3E8FD) to Color(0xFF9334E6), // 紫
    Color(0xFFE0F7FA) to Color(0xFF00838F), // 青
    Color(0xFFFBE9E7) to Color(0xFFD84315), // 橙
    Color(0xFFFCE4EC) to Color(0xFFC2185B), // 粉
)

@Composable
private fun TopicSubjectHeader(topic: Topic) {
    val context = LocalContext.current
    Column(Modifier.padding(top = 4.dp, bottom = 12.dp)) {
        // 主题标题：大号加粗，置于头部最上方。
        Text(
            topic.subject.content.ifEmpty { L.str(context, "Untitled") },
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                lineHeight = 34.sp,
            ),
        )

        // 话题与版块：位于标题下方，话题以圆角色块展示。
        val tags = topic.subject.tagsList
        if (tags.isNotEmpty() || topic.hasParentForum()) {
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (topic.hasParentForum()) {
                    Icon(
                        Icons.Filled.Bookmark,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                    Text(
                        topic.parentForum.name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MiuixTheme.colorScheme.primary,
                    )
                }
                tags.forEach { tag ->
                    val (chipBg, chipFg) =
                        TagChipPalette[abs(tag.hashCode()) % TagChipPalette.size]
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = chipBg,
                        border = BorderStroke(1.dp, chipFg.copy(alpha = 0.45f)),
                    ) {
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = chipFg,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        // 元信息行：回复数、浏览人数、发帖时间（各带小图标）。
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.ChatBubbleOutline,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                Text(
                    topic.repliesNum.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
                DateTimeText(timestampSeconds = topic.postDate)
            }
        }
    }
}

// endregion

// region Menus, banners, error states

@Composable
private fun TopicDetailsMoreMenu(
    topic: Topic,
    enableAuthorOnly: Boolean,
    localMode: Boolean,
    atForum: com.bugenzhao.mnga.protos.datamodel.Forum?,
    onAuthorOnly: () -> Unit,
    onViewCached: () -> Unit,
    onGotoForum: (com.bugenzhao.mnga.protos.datamodel.Forum) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onJump: (() -> Unit)? = null,
    onLoadFirstPage: (() -> Unit)? = null,
    onReply: (() -> Unit)? = null,
    onGotoTopic: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { open = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = null)
        }
        // Miuix menu (phase 3) replacing the Material DropdownMenu; the
        // grouped layout (id header, action groups, link group) is kept,
        // rendered with Miuix menu rows inside the Miuix popup panel.
        MiuixDropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            // Debug id header.
            MiuixMenuHeader(
                "#" + topic.id + (topic.fav.takeIf { it.isNotEmpty() }?.let { " @$it" } ?: ""),
            )
            MiuixMenuDivider()
            if (onJump != null) {
                MiuixMenuItem(
                    text = L.str(context, "Jump to..."),
                    icon = Icons.Filled.SwapVert,
                    onClick = { open = false; onJump() },
                )
            }
            if (onLoadFirstPage != null) {
                MiuixMenuItem(
                    text = L.str(context, "Load First Page"),
                    icon = Icons.Filled.ArrowUpward,
                    onClick = { open = false; onLoadFirstPage() },
                )
            }
            if (onReply != null) {
                MiuixMenuItem(
                    text = L.str(context, "Reply"),
                    icon = Icons.AutoMirrored.Filled.Reply,
                    onClick = { open = false; onReply() },
                )
            }
            if (onGotoTopic != null) {
                MiuixMenuItem(
                    text = L.str(context, "Goto Topic"),
                    icon = Icons.Filled.SubdirectoryArrowRight,
                    onClick = { open = false; onGotoTopic() },
                )
            }
            if (onJump != null || onLoadFirstPage != null || onReply != null || onGotoTopic != null) {
                MiuixMenuDivider()
            }
            if (enableAuthorOnly) {
                MiuixMenuItem(
                    text = L.str(context, "Author Only"),
                    icon = Icons.Filled.Person,
                    onClick = { open = false; onAuthorOnly() },
                )
            }
            if (!localMode) {
                MiuixMenuItem(
                    text = L.str(context, "View Cached Topic"),
                    icon = Icons.Filled.Bookmark,
                    onClick = { open = false; onViewCached() },
                )
            }
            if (onToggleFavorite != null) {
                MiuixMenuItem(
                    text = L.str(
                        context,
                        if (isFavorite) "Remove from Favorites" else "Mark as Favorite",
                    ),
                    icon = if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder,
                    onClick = { open = false; onToggleFavorite() },
                )
            }
            MiuixMenuDivider()
            val navID = NavigationIdentifier.TopicID(topic.id, topic.fav.takeIf { it.isNotEmpty() })
            MiuixMenuItem(
                text = L.str(context, "LumaGA Link"),
                icon = Icons.Filled.Bookmark,
                onClick = {
                    navID.mngaURL?.let {
                        val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE)
                            as? android.content.ClipboardManager
                        cm?.setPrimaryClip(
                            android.content.ClipData.newPlainText("泥坛", it)
                        )
                    }
                    open = false
                },
            )
            MiuixMenuItem(
                text = L.str(context, "Open in Browser"),
                icon = Icons.Filled.OpenInBrowser,
                onClick = { open = false; openInBrowser(context, topic) },
            )
            if (atForum != null) {
                MiuixMenuItem(
                    text = L.str(context, "Goto Forum") + " " + atForum.name,
                    icon = Icons.Filled.SwapVert,
                    onClick = { open = false; onGotoForum(atForum) },
                )
            }
        }
    }
}

/** Banner shown when the replies come from the local cache. */
@Composable
private fun LocalCacheBanner(reason: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MiuixTheme.colorScheme.surfaceVariant,
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            if (reason.isEmpty()) {
                L.str(context, "Cached Topic")
            } else {
                L.str(context, "Cached Topic") + " · " + reason
            },
            style = MaterialTheme.typography.labelMedium,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
    }
}

/** Content shown when the XML API response cannot be parsed (e.g. banned). */
@Composable
private fun XMLParseErrorMain(
    topic: Topic,
    onRefresh: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MiuixTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TopicSubjectHeader(topic = topic)
            Button(onClick = onRefresh) {
                Icon(Icons.Filled.Refresh, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(L.str(context, "Refresh"))
            }
            Button(onClick = onOpenInBrowser) {
                Icon(Icons.Filled.OpenInBrowser, null, Modifier.size(18.dp))
                Spacer(Modifier.size(6.dp))
                Text(L.str(context, "Open in Browser"))
            }
        }
    }
}

// endregion

// region Reply chain overlay

/** Full-screen list of a reply chain, ported from `PostReplyChainView`. */
@Composable
private fun ReplyChainOverlay(
    chain: List<PostId>,
    topic: Topic,
    resolver: QuotedPostResolver,
    votes: VotesModel,
    viewingImage: ViewingImageModel,
    onDismiss: () -> Unit,
    onNavigateAuthorOnly: (Post) -> Unit,
) {
    val context = LocalContext.current
    val posts by resolver.posts.collectAsState()
    val failed by resolver.failed.collectAsState()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
        ),
    ) {
        Surface(color = MiuixTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                    Text(
                        L.str(context, "Replies"),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { resolver.resetFailures() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(chain, key = { _, id -> "${id.tid}_${id.pid}" }) { _, id ->
                        val post = posts[id]
                        when {
                            post != null -> PostRow(
                                post = post,
                                isAuthor = post.authorId == topic.authorId,
                                votes = votes,
                                quotedResolver = resolver,
                                viewingImage = viewingImage,
                                enableAuthorOnly = false,
                                showMenu = false,
                                onNavigateAuthorOnly = onNavigateAuthorOnly,
                            )

                            id in failed -> Box(Modifier.fillMaxWidth().padding(24.dp)) {
                                Text(
                                    L.str(
                                        context,
                                        "Reply not found. It may have been deleted.",
                                    ),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }

                            else -> {
                                LaunchedEffect(id) { resolver.load(id) }
                                com.bugenzhao.mnga.ui.components.LoadingRow()
                            }
                        }
                    }
                }
            }
        }
    }
}

// endregion
