package com.bugenzhao.mnga.ui.components

import android.view.View
import com.bugenzhao.mnga.logicCallAsync
import com.bugenzhao.mnga.protos.service.AsyncRequest
import com.bugenzhao.mnga.protos.service.TopicFavorRequest
import com.bugenzhao.mnga.protos.service.TopicFavorResponse
import com.bugenzhao.mnga.util.Haptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Topic favoring: the `topicFavor` call behind the favorite toggles
 * (the topic screen's star button and menus).
 *
 * History: this file also held `SwipeToFavorBox`, the swipe-to-favorite
 * row wrapper whose swipe background was a star icon. The flat rows of
 * v1.8.0 lost the opaque card that used to hide that background star,
 * so it leaked onto every row (v1.8.2 gated it on the swipe progress);
 * the user then asked for the star gone outright, and v1.8.3 removes
 * the wrapper and the gesture with it — topic rows are plain rows
 * again, and favoriting lives in the topic screen's star button / menu
 * via [toggleTopicFavor].
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
