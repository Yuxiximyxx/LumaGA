package com.bugenzhao.mnga.ui.screens.topicdetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.util.Constants
import com.bugenzhao.mnga.util.L

/**
 * Jump-to-floor/page selector, ported from
 * `Shared/Views/TopicJumpSelectorView.swift`. The selected floor is the only
 * source of truth; the page is derived (`(floor + 20) / 20`) and setting a page
 * maps back to `(page - 1) * 20`.
 */
enum class TopicJumpSelectorMode(val label: String) {
    FLOOR("Floor"),
    PAGE("Page"),
}

@Composable
fun TopicJumpSelector(
    maxFloor: Int,
    mode: TopicJumpSelectorMode,
    initialFloor: Int,
    onModeChange: (TopicJumpSelectorMode) -> Unit,
    onDismiss: () -> Unit,
    onJump: (floor: Int, page: Int) -> Unit,
) {
    val context = LocalContext.current
    val maxPage = (maxFloor + Constants.postPerPage) / Constants.postPerPage

    // The only source of truth, clamped to the valid floor range.
    var selectedFloor by remember {
        mutableIntStateOf(initialFloor.coerceIn(0, maxOf(maxFloor, 0)))
    }
    var floorText by remember { mutableStateOf(selectedFloor.toString()) }
    var pageText by remember { mutableStateOf(selectedPage(selectedFloor).toString()) }

    // Keep the text fields in sync when the other control drives the change.
    LaunchedEffect(selectedFloor) {
        floorText = selectedFloor.toString()
        pageText = selectedPage(selectedFloor).toString()
    }

    // v1.8.5: presented as an IN-WINDOW overlay, no longer as a Miuix
    // SuperBottomSheet. On device (v1.8.4 and earlier) both jump
    // entries — the ⋮ menu item and the header title tap — flipped
    // the screen's showJumpSelector state and this composable was
    // composed, yet nothing ever appeared: the chain was intact up
    // to this presentation step (the state, both click paths and the
    // onJump -> floorToJump/loadFromPage landing are shared with
    // working features and check out in code), and the sheet was the
    // one step living outside this window — SuperBottomSheet is
    // composed in its own Dialog window (see the audit in
    // ui/root/ScreenBackdrop.kt), and every sheet in the app that is
    // known to open on device is hosted at the root (the editor, via
    // GlobalSheets in LumaGARoot), not inside a navigation entry.
    // The overlay below is composed in this screen's own composition,
    // dismisses on scrim tap / back, and keeps the exact same
    // content and onJump/onDismiss contract. (The BackHandler only
    // dismisses this overlay; it never calls pop, so it does not
    // clash with the root-owned navigation back handling.)
    BackHandler(enabled = true, onBack = onDismiss)
    Box(Modifier.fillMaxSize().imePadding()) {
        Box(
            Modifier.fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss),
        )
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(MiuixTheme.colorScheme.surface)
                // Consume taps on the panel's own padding so they do
                // not fall through to the dismiss scrim behind it.
                .clickable(
                    indication = null,
                    interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    },
                ) {}
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally)
                    .padding(top = 8.dp, bottom = 4.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(
                        MiuixTheme.colorScheme.onSurfaceVariantSummary
                            .copy(alpha = 0.4f)
                    ),
            )
            TabRow(
                tabs = TopicJumpSelectorMode.entries.map { L.str(context, it.label) },
                selectedTabIndex = mode.ordinal,
                onTabSelected = { onModeChange(TopicJumpSelectorMode.entries[it]) },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.padding(8.dp))
            Text(
                L.str(context, "Jump to..."),
                style = MaterialTheme.typography.labelMedium,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
            Spacer(Modifier.padding(4.dp))

            when (mode) {
                TopicJumpSelectorMode.FLOOR -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L.str(context, "Floor"))
                        Spacer(Modifier.weight(1f))
                        NumberField(
                            value = floorText,
                            onValueChange = { text ->
                                floorText = text
                                text.toIntOrNull()?.let {
                                    selectedFloor = it.coerceIn(0, maxOf(maxFloor, 0))
                                }
                            },
                        )
                    }
                    if (maxFloor > 0) {
                        RangeCaption("0", maxFloor.toString())
                        Slider(
                            value = selectedFloor.toFloat(),
                            onValueChange = { selectedFloor = it.toInt() },
                            valueRange = 0f..maxFloor.toFloat(),
                            steps = maxOf(maxFloor - 1, 0),
                        )
                    }
                }

                TopicJumpSelectorMode.PAGE -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L.str(context, "Page"))
                        Spacer(Modifier.weight(1f))
                        NumberField(
                            value = pageText,
                            onValueChange = { text ->
                                pageText = text
                                text.toIntOrNull()?.let { page ->
                                    val clamped = page.coerceIn(1, maxOf(maxPage, 1))
                                    selectedFloor = (clamped - 1) * Constants.postPerPage
                                }
                            },
                        )
                    }
                    if (maxPage > 1) {
                        RangeCaption("1", maxPage.toString())
                        Slider(
                            value = selectedPage(selectedFloor).toFloat(),
                            onValueChange = {
                                selectedFloor = (it.toInt() - 1) * Constants.postPerPage
                            },
                            valueRange = 1f..maxPage.toFloat(),
                            steps = maxOf(maxPage - 2, 0),
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = L.str(context, "Jump"),
                    onClick = {
                        onJump(selectedFloor, selectedPage(selectedFloor))
                        onDismiss()
                    },
                )
            }
        }
    }
}

private fun selectedPage(floor: Int): Int = (floor + Constants.postPerPage) / Constants.postPerPage

@Composable
private fun RangeCaption(start: String, end: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            start,
            style = MaterialTheme.typography.labelSmall,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
        Spacer(Modifier.weight(1f))
        Text(
            end,
            style = MaterialTheme.typography.labelSmall,
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        )
    }
}

/** Number-pad text field, trailing aligned. */
@Composable
private fun NumberField(value: String, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier
            .widthIn(min = 120.dp)
            .heightIn(min = 56.dp),
        backgroundColor = MiuixTheme.colorScheme.surfaceContainerHigh,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(textAlign = TextAlign.End),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    )
}
