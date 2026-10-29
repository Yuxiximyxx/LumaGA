package com.bugenzhao.mnga.ui.components

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.logicCallAsync
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.protos.service.TopicFavorRequest
import com.bugenzhao.mnga.protos.service.TopicFavorResponse
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Topic favoring, shared by the screens that can favorite a topic: the
 * `topicFavor` call behind it and the swipe-to-favorite row wrapper. Extracted
 * from `TopicListView`'s item wrapper so the search results offer the same
 * gesture.
 */

/**
 * Toggles the topic's server-side favorite state through `topicFavor` and
 * reports the settled state through [onResult]; the caller keeps it as a local
 * override so its rows update without a refetch.
 */
fun toggleTopicFavor(
    scope: CoroutineScope,
    view: View,
    topicId: String,
    currentFavored: Boolean,
    onResult: (Boolean) -> Unit,
    onFailure: () -> Unit = {},
) {
    val operation = if (currentFavored) {
        TopicFavorRequest.Operation.DELETE
    } else {
        TopicFavorRequest.Operation.ADD
    }
    scope.launch {
        val result = logicCallAsync(
            AsyncRequest.newBuilder()
                .setTopicFavor(
                    TopicFavorRequest.newBuilder()
                        .setTopicId(topicId)
                        .setOperation(operation)
                        .build()
                )
                .build(),
            TopicFavorResponse.parser(),
        )
        result.onSuccess { response ->
            onResult(response.isFavored)
            Haptics.play(view, Haptics.NotificationType.SUCCESS)
        }.onFailure {
            onFailure()
        }
    }
}

/**
 * The swipe-to-favorite row wrapper: a swipe from the start edge triggers
 * [onFavor] and the row snaps back — favoriting keeps the row.
 *
 * Miuix 0.8.8 has no swipe-to-dismiss equivalent, so the Material3
 * [SwipeToDismissBox] is retained; only the background [Icon] and its tint
 * are Miuix.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToFavorBox(
    onFavor: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val context = LocalContext.current
    val boxState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onFavor()
            }
            false // Never actually dismiss: favoriting keeps the row.
        }
    )

    SwipeToDismissBox(
        state = boxState,
        modifier = modifier,
        backgroundContent = {
            // Gate the star on the swipe progress (v1.8.2): it used to be
            // drawn unconditionally and was only hidden because the classic
            // row painted an opaque Card over this background layer. The
            // v1.8.0 flat rows have no opaque background of their own (the
            // grouped card surface sits UNDER this dismiss box), so the
            // star leaked through the row's transparent areas and showed
            // on every row next to the avatar. At rest progress is 0 and
            // nothing is drawn; the star fades in only while swiping.
            val progress = boxState.progress
            if (progress > 0.01f) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = L.str(context, "Mark as Favorite"),
                        tint = MiuixTheme.colorScheme.primary.copy(
                            alpha = progress.coerceIn(0f, 1f),
                        ),
                        modifier = Modifier.padding(start = 32.dp),
                    )
                }
            }
        },
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        content = content,
    )
}
