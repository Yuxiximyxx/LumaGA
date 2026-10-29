package com.bugenzhao.mnga.ui.screens.notifications

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.AddComment
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Comment
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.MarkEmailRead
import androidx.compose.material.icons.outlined.MarkEmailUnread
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
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.logicCall
import com.bugenzhao.mnga.protos.datamodel.Notification
import com.bugenzhao.mnga.protos.service.MarkNotificationReadRequest
import com.bugenzhao.mnga.protos.service.SyncRequest
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.nav.RouteDestination
import com.xah.container.component.base.sharedContainer
import com.bugenzhao.mnga.ui.screens.user.nameDisplayCompat
import com.bugenzhao.mnga.util.DateFormatters
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Icon per notification type, mirroring `Notification.TypeEnum.icon`.
 */
private fun Notification.Type.iconVector(): ImageVector = when (this) {
    Notification.Type.REPLY_POST, Notification.Type.REPLY_TOPIC ->
        Icons.AutoMirrored.Filled.Reply
    Notification.Type.VOTE -> Icons.Filled.ThumbUp
    Notification.Type.SHORT_MESSAGE -> Icons.AutoMirrored.Filled.Chat
    Notification.Type.SHORT_MESSAGE_START -> Icons.Outlined.AddComment
    Notification.Type.AT_POST, Notification.Type.AT_TOPIC -> Icons.Outlined.AlternateEmail
    Notification.Type.UNKNOWN, Notification.Type.UNRECOGNIZED ->
        Icons.Outlined.HelpOutline
}

/** Localized phrase per notification type, mirroring `.description`. */
private fun Notification.Type.descriptionKey(): String = when (this) {
    Notification.Type.REPLY_POST -> "replied to your post"
    Notification.Type.REPLY_TOPIC -> "replied to your topic"
    Notification.Type.VOTE -> "received 10 more votes"
    Notification.Type.SHORT_MESSAGE, Notification.Type.SHORT_MESSAGE_START ->
        "sent you a short message"
    Notification.Type.AT_POST -> "mentioned you in a post"
    Notification.Type.AT_TOPIC -> "mentioned you in a topic"
    Notification.Type.UNKNOWN, Notification.Type.UNRECOGNIZED -> ""
}

/** Whether the notification's actor is shown in the footer. */
private fun Notification.Type.showsOtherUser(): Boolean = when (this) {
    Notification.Type.REPLY_POST, Notification.Type.REPLY_TOPIC,
    Notification.Type.SHORT_MESSAGE, Notification.Type.SHORT_MESSAGE_START,
    Notification.Type.AT_POST, Notification.Type.AT_TOPIC,
    -> true
    else -> false
}

/**
 * The notifications sheet, ported from `NotificationListView` +
 * `NotificationListNavigationView`: paged rows with swipe-to-toggle read,
 * optimistic marking and Mark All as Read.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationListSheet(
    navigator: Navigator,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()

    val dataSource = App.notis.dataSource
    val state by dataSource.state.collectAsState()

    // Optimistic read overrides applied on top of the fetched list.
    val readOverrides = remember { mutableStateMapOf<String, Boolean>() }

    fun isRead(noti: Notification): Boolean = readOverrides[noti.id] ?: noti.read
    fun mark(ids: List<String>, read: Boolean, onSuccess: () -> Unit = {}) {
        if (ids.isEmpty()) return
        scope.launch {
            withContext(Dispatchers.IO) {
                runCatching {
                    logicCall(
                        SyncRequest.newBuilder()
                            .setMarkNotiRead(
                                MarkNotificationReadRequest.newBuilder()
                                    .addAllIds(ids)
                                    .setRead(read)
                            )
                            .build(),
                    )
                }
            }
            ids.forEach { readOverrides[it] = read }
            onSuccess()
        }
    }

    LaunchedEffect(Unit) {
        if (dataSource.notLoaded) dataSource.initialLoad()
    }

    val unreadCount = state.items.count { isRead(it).not() }

    // 页面形式（与设置页一致的 AppBar），不再是底部弹窗。
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            // Miuix 0.8.8 SmallTopAppBar has no subtitle slot; the unread
            // count subtitle is folded into the list header below (same
            // approach as TopicListScreen).
            SmallTopAppBar(
                color = Color.Transparent, // v1.8.5: no solid top band
                title = L.str(context, "Notifications"),
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null,
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            mark(
                                state.items.filter { !isRead(it) }.map { it.id },
                                read = true,
                            ) { Haptics.play(view, Haptics.NotificationType.SUCCESS) }
                        },
                        enabled = unreadCount > 0,
                    ) {
                        Icon(
                            Icons.Filled.Check,
                            contentDescription = L.str(context, "Mark All as Read"),
                        )
                    }
                },
            )
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = state.isRefreshing,
            onRefresh = { dataSource.refreshAsync(sleepMillis = 500) },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                dataSource.isInitialLoading || dataSource.notLoaded ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                state.items.isEmpty() ->
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            L.str(context, "All Read"),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                else -> {
                    val listState = rememberLazyListState()
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "unread-header") {
                            Text(
                                if (unreadCount > 0) {
                                    L.str(context, "%lld Unread", unreadCount)
                                } else {
                                    L.str(context, "All Read")
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                        itemsIndexed(state.items, key = { _, n -> n.id }) { _, noti ->
                            val read = isRead(noti)
                            val targetRoute = routeForNotification(noti)
                            NotificationRow(
                                noti = noti,
                                read = read,
                                // Morph the row into its target page
                                // (SharedNav shared container; the key
                                // follows the per-type destination).
                                modifier = Modifier.sharedContainer(
                                    key = RouteDestination.keyFor(targetRoute),
                                    shape = RoundedCornerShape(12.dp),
                                    containerColor = null,
                                ),
                                onClick = {
                                    mark(listOf(noti.id), read = true)
                                    // 与其它列表页一致：push 详情，返回时回到通知列表。
                                    navigator.push(targetRoute)
                                },
                                onToggleRead = { mark(listOf(noti.id), read = !read) },
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Route per `NotificationListView.buildLink`. */
private fun routeForNotification(noti: Notification): Route = when (noti.type) {
    Notification.Type.SHORT_MESSAGE, Notification.Type.SHORT_MESSAGE_START ->
        // The SM conversation id is carried in `otherPostID.tid`.
        Route.ShortMessageDetails(id = noti.otherPostId.tid)
    else -> Route.TopicDetails(
        topicId = noti.otherPostId.tid,
        postId = noti.otherPostId.pid.takeIf { it.isNotEmpty() },
        startPage = noti.page.toInt().takeIf { it > 0 } ?: 1,
    )
}

/** One notification row, ported from `NotificationRowView`. */
@Composable
private fun NotificationRow(
    noti: Notification,
    read: Boolean,
    onClick: () -> Unit,
    onToggleRead: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val foreground =
        if (read) MiuixTheme.colorScheme.onSurfaceVariantSummary
        else MiuixTheme.colorScheme.onSurface

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MiuixTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    noti.type.iconVector(),
                    contentDescription = null,
                    tint = foreground,
                )
                Text(
                    subjectText(context, noti),
                    style = MaterialTheme.typography.bodyLarge,
                    color = foreground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (!read) UnreadDot()
                IconButton(onClick = onToggleRead, modifier = Modifier.padding(0.dp)) {
                    Icon(
                        if (read) Icons.Outlined.MarkEmailUnread
                        else Icons.Outlined.MarkEmailRead,
                        contentDescription = L.str(context, if (read) "Unread" else "Read"),
                        tint = MiuixTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (noti.type.showsOtherUser()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            noti.otherUser.nameDisplayCompat.ifEmpty {
                                noti.otherUser.name.normal
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                } else if (noti.type == Notification.Type.VOTE) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Outlined.Comment,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Text(
                            L.str(context, "Your post"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                val description = L.str(context, noti.type.descriptionKey())
                if (description.isNotEmpty()) {
                    Text(
                        description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
                Box(Modifier.weight(1f))
                Text(
                    DateFormatters.automatic(context, Date(noti.timestamp * 1000)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}

/** SM types render a synthetic subject; others use the topic subject. */
private fun subjectText(context: android.content.Context, noti: Notification): String =
    when (noti.type) {
        Notification.Type.SHORT_MESSAGE, Notification.Type.SHORT_MESSAGE_START ->
            L.str(context, "Short Message")
        else -> noti.topicSubject.content.ifEmpty {
            noti.topicSubject.tagsList.joinToString(" ")
        }
    }

/** Small unread indicator dot. */
@Composable
private fun UnreadDot() {
    Box(
        Modifier
            .padding(2.dp)
            .size(8.dp)
            .background(MiuixTheme.colorScheme.primary, CircleShape),
    )
}