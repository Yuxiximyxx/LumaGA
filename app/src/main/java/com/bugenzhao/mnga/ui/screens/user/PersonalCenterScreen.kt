package com.bugenzhao.mnga.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAddAlt1
import androidx.compose.material.icons.filled.PersonRemoveAlt1
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.model.PlusFeature
import com.bugenzhao.mnga.protos.datamodel.AuthInfo
import com.bugenzhao.mnga.protos.datamodel.User
import com.bugenzhao.mnga.storage.BlockWordsStorage
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.root.LocalDockBackdrop
import com.bugenzhao.mnga.ui.root.ScreenBackdropProvider
import com.bugenzhao.mnga.ui.root.rememberDockBackdropSourceModifier
import com.bugenzhao.mnga.ui.root.tabDockContentBottomPadding
import com.bugenzhao.mnga.ui.nav.RouteDestination
import com.xah.container.component.base.sharedContainer
import com.bugenzhao.mnga.util.L
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Personal center page: the former account bottom sheet (`UserMenuSheet`),
 * repackaged as a full page. Since the MIUIX phase-2 shell it is the
 * bottom-bar "Mine" tab root, so it has no back button; its content
 * (current user card, quick links, account switcher, sign in/out and app
 * settings) is unchanged from the drawer-page era.
 */
/**
 * Entry point: publishes the dock backdrop — recorded by this
 * screen's own list below — as the screen backdrop (v1.7.6). This
 * screen has no popup menus of its own; publishing it lets root-level
 * consumers (toasts) sample this tab's content like every other
 * participating screen.
 */
@Composable
fun PersonalCenterScreen(navigator: Navigator) {
    ScreenBackdropProvider(LocalDockBackdrop.current) {
        PersonalCenterScreenContent(navigator)
    }
}

@Composable
private fun PersonalCenterScreenContent(navigator: Navigator) {
    val context = LocalContext.current
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            TopAppBar(
                title = L.str(context, "Personal Center"),
            )
        },
    ) { padding ->
        // While this tab root is current, its own scrolling content is
        // the tab dock's glass sample source (see DockBackdrop.kt).
        val navStack by navigator.stack.collectAsState()
        val dockSource = rememberDockBackdropSourceModifier(
            navStack.lastOrNull() is Route.PersonalCenter,
        )
        PersonalCenterContent(
            navigator = navigator,
            modifier = Modifier.fillMaxSize().padding(padding).then(dockSource),
        )
    }
}

@Composable
private fun PersonalCenterContent(
    navigator: Navigator,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val authInfo by App.authStorage.authInfo.collectAsState()
    val allAuthInfos by App.authStorage.allAuthInfos.collectAsState()
    val currentUser by App.currentUser.user.collectAsState()
    val unreadCount by App.notis.unreadCountAnimated.collectAsState()
    val blockWords by App.blockWords.words.collectAsState()
    val blockedUserCount = blockWords.count { it.word.startsWith(BlockWordsStorage.userPrefix) }

    val signedIn = authInfo.token.isNotEmpty()

    LazyColumn(
        modifier.padding(horizontal = 16.dp),
        // Bottom clears the floating tab dock (tab-root screen).
        contentPadding = PaddingValues(top = 12.dp, bottom = tabDockContentBottomPadding()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Current user card.
        item(key = "current-user") {
            MenuCard {
                if (signedIn) {
                    CurrentUserCard(
                        user = currentUser,
                        // Morph the current-user card into the profile
                        // page (SharedNav shared container; key follows
                        // the profile destination for this user).
                        sharedContainerKey = currentUser?.let {
                            RouteDestination.keyFor(Route.UserProfile(user = it))
                        },
                    ) {
                        currentUser?.let { user ->
                            navigator.push(Route.UserProfile(user = user))
                        }
                    }
                } else {
                    MenuRow(
                        icon = Icons.Filled.Person,
                        title = L.str(context, "Sign in to NGA"),
                        subtitle = L.str(
                            context,
                            "Sign in to NGA and discover more features here.",
                        ),
                    ) {
                        App.authStorage.setIsSigning(true)
                    }
                }
            }
        }

        if (signedIn) {
            item(key = "section-quick") {
                MenuCard {
                    MenuRow(
                        icon = Icons.Filled.Notifications,
                        title = L.str(context, "Notifications"),
                        trailing = {
                            if (unreadCount > 0) {
                                CountBadge(
                                    text = unreadCount.toString(),
                                    containerColor = MiuixTheme.colorScheme.primary,
                                    contentColor = MiuixTheme.colorScheme.onPrimary,
                                )
                            }
                        },
                    ) {
                        navigator.push(Route.Notifications)
                    }
                    MenuRow(
                        icon = Icons.AutoMirrored.Filled.Chat,
                        title = L.str(context, "Short Messages"),
                    ) {
                        if (checkPlusFeature(PlusFeature.SHORT_MESSAGE)) {
                            navigator.push(Route.ShortMessages)
                        }
                    }
                    MenuRow(
                        icon = Icons.Outlined.Badge,
                        title = L.str(context, "My Profile"),
                    ) {
                        currentUser?.let { user ->
                            navigator.push(Route.UserProfile(user = user))
                        }
                    }
                    MenuRow(
                        icon = Icons.Filled.Bookmark,
                        title = L.str(context, "Favorite Topics"),
                    ) {
                        // Favorites is a dock tab root (v1.8.0): switch
                        // tabs instead of stacking it over Mine.
                        navigator.selectTab(Route.Favorites)
                    }
                    MenuRow(
                        icon = Icons.Filled.HistoryEdu,
                        title = L.str(context, "History"),
                    ) {
                        if (checkPlusFeature(PlusFeature.TOPIC_HISTORY)) {
                            navigator.push(Route.History)
                        }
                    }
                    MenuRow(
                        icon = Icons.Outlined.Shield,
                        title = L.str(context, "Block Management"),
                        trailing = if (blockedUserCount > 0) {
                            {
                                CountBadge(
                                    text = blockedUserCount.toString(),
                                    containerColor = MiuixTheme.colorScheme.surfaceContainerHigh,
                                    contentColor = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        } else null,
                    ) {
                        navigator.push(Route.BlockedUsers)
                    }
                }
            }
        }

        // Account switcher.
        item(key = "section-accounts") {
            MenuCard {
                SectionLabel(L.str(context, "Accounts"))
                val accounts = allAuthInfos.sortedBy { it.uid }
                accounts.forEach { info ->
                    key("acc-${info.uid}") {
                        AccountRow(
                            info = info,
                            selected = info.uid == authInfo.uid,
                        ) {
                            if (checkPlusFeature(PlusFeature.MULTI_ACCOUNT)) {
                                App.authStorage.setCurrentAuth(info)
                            }
                        }
                    }
                }
                // 实验室功能「启用签到」：账号分区里的签到入口（点击进签到页）。
                if (App.prefs.clockInEnabled.value) {
                    val clockedIn by App.currentUser.todayClockedIn.collectAsState()
                    LaunchedEffect(Unit) {
                        App.currentUser.refreshTodayClockIn()
                        App.currentUser.queryClockInStats()
                    }
                    MenuRow(
                        icon = Icons.Filled.CheckCircle,
                        title = L.str(context, "Clock In"),
                        trailing = if (clockedIn) {
                            {
                                Text(
                                    L.str(context, "Clocked In"),
                                    style = MiuixTheme.textStyles.body2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        } else null,
                    ) {
                        navigator.push(Route.ClockIn)
                    }
                }
                MenuRow(
                    icon = Icons.Filled.PersonAddAlt1,
                    title = L.str(context, "Add Account"),
                ) {
                    if (checkPlusFeature(PlusFeature.MULTI_ACCOUNT)) {
                        App.authStorage.setIsSigning(true)
                    }
                }
                if (signedIn) {
                    MenuRow(
                        icon = Icons.Filled.PersonRemoveAlt1,
                        title = L.str(context, "Sign Out"),
                        tint = MiuixTheme.colorScheme.error,
                    ) {
                        App.authStorage.clearCurrentAuth()
                    }
                }
            }
        }

        // App settings entry.
        item(key = "section-links") {
            MenuCard {
                MenuRow(
                    icon = Icons.Filled.Settings,
                    title = L.str(context, "Settings"),
                ) {
                    navigator.push(Route.Settings)
                }
            }
        }

        item(key = "footer-spacing") {
            Box(Modifier.size(12.dp))
        }
    }
}

/** Count pill shown at a row's trailing edge (unread / blocked counts). */
@Composable
private fun CountBadge(text: String, containerColor: Color, contentColor: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor),
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
            style = MiuixTheme.textStyles.footnote1,
            color = contentColor,
        )
    }
}

/** Grouped card container, same Miuix Card recipe as the shared GroupedList. */
@Composable
private fun MenuCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(0.dp),
    ) {
        Column { content() }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun MenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tint: Color? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val iconTint = tint ?: MiuixTheme.colorScheme.onSurfaceVariantSummary
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, contentDescription = null, tint = iconTint)
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MiuixTheme.textStyles.body1,
                color = tint ?: MiuixTheme.colorScheme.onSurface,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
        if (trailing != null) trailing()
    }
}

@Composable
private fun AccountRow(
    info: AuthInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            if (selected) Icons.Filled.Person else Icons.Outlined.Badge,
            contentDescription = null,
            tint = if (selected) MiuixTheme.colorScheme.primary
            else MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Text(
            info.cachedName.ifEmpty { info.uid.ifEmpty { L.str(context, "Unknown") } },
            style = MiuixTheme.textStyles.body1,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Text(
                L.str(context, "Current Version"),
                style = MiuixTheme.textStyles.footnote1,
                color = MiuixTheme.colorScheme.primary,
            )
        }
    }
}

@Composable
private fun CurrentUserCard(
    user: User?,
    sharedContainerKey: String? = null,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .then(
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
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        UserView(
            user = user ?: User.getDefaultInstance(),
            style = UserViewStyle.NORMAL,
            modifier = Modifier.weight(1f),
        )
        Icon(
            Icons.Filled.Group,
            contentDescription = L.str(context, "Accounts"),
            tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}
