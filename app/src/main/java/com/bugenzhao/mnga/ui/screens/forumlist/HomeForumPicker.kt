package com.bugenzhao.mnga.ui.screens.forumlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.util.L
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Searchable home-forum picker (v1.8.2). The primary entry moved from
 * Settings to the home tab's top-left button (both home forms — the
 * default forum-list home and a customized forum tab root); the
 * Settings row remains as a secondary entry. A search field at the top
 * filters the full forum catalog by name in real time, so any forum
 * can be set as the home tab without scrolling the whole directory.
 * "默认主页" clears the customization.
 */
@Composable
fun HomeForumPickerDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val forumListVM: ForumListViewModel = viewModel()
    val forumListState by forumListVM.dataSource.state.collectAsState()
    val homeForum by App.homeForum.forum.collectAsState()

    LaunchedEffect(forumListVM) {
        if (forumListVM.dataSource.notLoaded) {
            forumListVM.dataSource.initialLoad()
        }
    }

    var query by remember { mutableStateOf("") }
    val allForums = remember(forumListState.items) {
        forumListState.items
            .filterNot { it.id == "mnga" }
            .flatMap { it.forumsList }
            .distinctBy { forumIdKey(it.id) }
    }
    val filtered = remember(allForums, query) {
        val q = query.trim()
        if (q.isEmpty()) allForums
        else allForums.filter { it.name.contains(q, ignoreCase = true) }
    }
    val selectedKey = homeForum?.let { forumIdKey(it.id) }

    WindowDialog(
        show = true,
        title = L.str(context, "Home Forum"),
        onDismissRequest = onDismiss,
    ) {
        Column {
            TextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                label = L.str(context, "Search"),
                useLabelAsPlaceholder = true,
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null)
                },
            )
            LazyColumn(Modifier.heightIn(max = 400.dp)) {
                item(key = "default-home") {
                    HomeForumPickerRow(
                        label = L.str(context, "Default Home"),
                        selected = selectedKey == null,
                        onClick = {
                            App.homeForum.set(null)
                            onDismiss()
                        },
                    )
                }
                items(filtered, key = { forumIdKey(it.id) }) { forum ->
                    HomeForumPickerRow(
                        label = forum.name,
                        selected = selectedKey == forumIdKey(forum.id),
                        onClick = {
                            App.homeForum.set(forum)
                            onDismiss()
                        },
                    )
                }
            }
            if (filtered.isEmpty() && allForums.isNotEmpty()) {
                Text(
                    "没有匹配的版面",
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = L.str(context, "Cancel"),
                    onClick = onDismiss,
                )
            }
        }
    }
}

/** One radio row of [HomeForumPickerDialog], mirroring the Settings picker. */
@Composable
private fun HomeForumPickerRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label)
    }
}
