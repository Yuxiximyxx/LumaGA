package com.bugenzhao.mnga.ui.screens.user

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.protos.datamodel.BlockWord
import com.bugenzhao.mnga.storage.BlockWordsStorage
import com.bugenzhao.mnga.ui.components.InitialAvatar
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L

/** A focused roster for reviewing and removing user blocks. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockedUsersScreen(navigator: Navigator) {
    val context = LocalContext.current
    val view = LocalView.current
    val storage = App.blockWords
    val words by storage.words.collectAsState()
    val blockedUsers = remember(words) {
        words.mapNotNull { word ->
            word.word.takeIf { it.startsWith(BlockWordsStorage.userPrefix) }
                ?.removePrefix(BlockWordsStorage.userPrefix)
                ?.takeIf { it.isNotBlank() }
                ?.let { name -> BlockedUser(word, name) }
        }.sortedBy { it.name.lowercase() }
    }
    var confirmUnblockAll by remember { mutableStateOf(false) }

    fun unblock(word: BlockWord) {
        storage.remove(word)
        Haptics.play(view, Haptics.NotificationType.SUCCESS)
    }

    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            SmallTopAppBar(
                title = L.str(context, "Block Management"),
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "privacy-summary") {
                BlockedUsersSummary(
                    count = blockedUsers.size,
                    onUnblockAll = if (blockedUsers.size > 1) {
                        { confirmUnblockAll = true }
                    } else null,
                )
            }

            if (blockedUsers.isEmpty()) {
                item(key = "empty") { BlockedUsersEmptyState() }
            } else {
                item(key = "section-label") {
                    Text(
                        L.str(context, "Blocked Users"),
                        style = MaterialTheme.typography.labelLarge,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(start = 4.dp, top = 4.dp),
                    )
                }
                items(blockedUsers, key = { it.word.word }) { blockedUser ->
                    BlockedUserRow(
                        name = blockedUser.name,
                        onUnblock = { unblock(blockedUser.word) },
                    )
                }
            }
        }
    }

    if (confirmUnblockAll) {
        // Miuix WindowDialog has no icon slot; the shield icon moves into
        // the content, and title/summary carry the old title/text.
        WindowDialog(
            show = true,
            title = L.str(context, "Unblock All Users?"),
            summary = L.str(context, "Their topics and replies will be visible again."),
            onDismissRequest = { confirmUnblockAll = false },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Shield, contentDescription = null)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        text = L.str(context, "Cancel"),
                        onClick = { confirmUnblockAll = false },
                    )
                    TextButton(
                        text = L.str(context, "Unblock All"),
                        onClick = {
                            blockedUsers.forEach { storage.remove(it.word) }
                            Haptics.play(view, Haptics.NotificationType.SUCCESS)
                            confirmUnblockAll = false
                        },
                    )
                }
            }
        }
    }
}

private data class BlockedUser(val word: BlockWord, val name: String)

@Composable
private fun BlockedUsersSummary(count: Int, onUnblockAll: (() -> Unit)?) {
    val context = LocalContext.current
    Surface(
        color = MiuixTheme.colorScheme.primaryContainer,
        contentColor = MiuixTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f),
            ) {
                Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Outlined.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    L.str(context, "%lld blocked users", count),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    L.str(context, "Topics and replies from these users are hidden."),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiuixTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.76f),
                )
            }
            if (onUnblockAll != null) {
                TextButton(
                    text = L.str(context, "Unblock All"),
                    onClick = onUnblockAll,
                )
            }
        }
    }
}

@Composable
private fun BlockedUserRow(name: String, onUnblock: () -> Unit) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MiuixTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            InitialAvatar(name = name, size = 42.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    L.str(context, "Topics and replies hidden"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
            // Miuix has no OutlinedButton; the low-weight TextButton is the
            // closest equivalent for this small row action.
            TextButton(
                text = L.str(context, "Unblock"),
                onClick = onUnblock,
                modifier = Modifier.height(36.dp),
            )
        }
    }
}

@Composable
private fun BlockedUsersEmptyState() {
    val context = LocalContext.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MiuixTheme.colorScheme.dividerLine),
        color = MiuixTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Outlined.VerifiedUser,
                contentDescription = null,
                tint = MiuixTheme.colorScheme.primary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                L.str(context, "No Blocked Users"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                L.str(context, "People you block will appear here."),
                style = MaterialTheme.typography.bodyMedium,
                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            )
        }
    }
}
