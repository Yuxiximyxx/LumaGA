package com.bugenzhao.mnga.ui.screens.forumlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.ui.nav.Route
import com.bugenzhao.mnga.ui.root.rememberDockBackdropSourceModifier
import com.bugenzhao.mnga.ui.screens.topiclist.TopicListScreen

/**
 * The 主页 tab root (v1.8.1): by default the classic forum-list home
 * ([ForumListScreen], unchanged from v1.8.0). When the user has
 * customized the home forum (设置 → 主页版面， or a forum row's
 * long-press menu), this tab instead opens that forum's topic list
 * directly as the tab root — the same [TopicListScreen] used for
 * pushed lists, in its tab-root mode (no back chevron, dock-aware
 * bottom padding and FAB placement).
 *
 * Switching the setting re-keys the topic list, so the new forum
 * starts from a fresh list state; clearing it returns to the default
 * home. The stack contract is untouched: this all lives *inside*
 * [Route.ForumList]'s destination, so [Navigator.selectTab], the
 * back lands-on-home rule and the dock's tab routing never see the
 * difference.
 */
@Composable
fun HomeTabScreen(navigator: Navigator) {
    val homeForum by App.homeForum.forum.collectAsState()
    val forum = homeForum
    if (forum == null) {
        ForumListScreen(navigator)
    } else {
        key(forumIdKey(forum.id)) {
            HomeForumTabRoot(navigator, forum)
        }
    }
}

@Composable
private fun HomeForumTabRoot(
    navigator: Navigator,
    forum: com.bugenzhao.mnga.protos.datamodel.Forum,
) {
    // Like every tab root, this screen records its own scrolling
    // content into the dock backdrop while it is the current
    // destination (the shell wrapper does not record on tab roots —
    // see DockBackdrop.kt). The recording wraps the topic list from
    // outside; the list's own FAB backdrop recording inside it is the
    // same nested arrangement pushed topic lists already live with
    // (shell recording + FAB recording).
    val navStack by navigator.stack.collectAsState()
    val dockSource = rememberDockBackdropSourceModifier(
        navStack.lastOrNull() is Route.ForumList,
    )
    Box(Modifier.fillMaxSize().then(dockSource)) {
        TopicListScreen(
            navigator,
            forumId = forum.id,
            asTabRoot = true,
        )
    }
}
