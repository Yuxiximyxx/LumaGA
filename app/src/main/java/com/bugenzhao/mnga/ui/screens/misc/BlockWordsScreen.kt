package com.bugenzhao.mnga.ui.screens.misc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.FrontHand
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.App
import com.bugenzhao.mnga.protos.datamodel.BlockWord
import com.bugenzhao.mnga.storage.BlockWordsStorage
import com.bugenzhao.mnga.ui.nav.Navigator
import com.bugenzhao.mnga.util.Haptics
import com.bugenzhao.mnga.util.L
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTopAppBar
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Block word editor, ported from `Views/BlockWordListView.swift`: keyword rows
 * (user blocks rendered as chips), swipe-to-delete and an add-word field.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BlockWordsScreen(navigator: Navigator? = null) {
    val context = LocalContext.current
    val view = LocalView.current
    val storage = App.blockWords
    val words by storage.words.collectAsState()

    var newWord by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    fun commitNewWord() {
        val pending = newWord
        if (!pending.isNullOrBlank()) {
            storage.add(BlockWord.newBuilder().setWord(pending.trim()).build())
            Haptics.play(view, Haptics.NotificationType.SUCCESS)
        }
        newWord = null
    }

    LaunchedEffect(newWord) {
        if (newWord != null) focusRequester.requestFocus()
    }


    Scaffold(
        contentWindowInsets = WindowInsets.systemBars.union(WindowInsets.displayCutout)
            .exclude(WindowInsets.navigationBars),
        topBar = {
            SmallTopAppBar(
                title = L.str(context, "Block Contents"),
                navigationIcon = {
                    IconButton(onClick = { navigator?.pop() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        val pending = newWord
                        if (!pending.isNullOrBlank()) {
                            storage.add(BlockWord.newBuilder().setWord(pending.trim()).build())
                        }
                        newWord = ""
                    }) {
                        Icon(Icons.Filled.Add, contentDescription = L.str(context, "Add Word"))
                    }
                },
            )
        },
    ) { padding ->
        if (words.isEmpty() && newWord == null) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Outlined.FrontHand,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    L.str(context, "No Block Words"),
                    style = MiuixTheme.textStyles.headline1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (newWord != null) {
                    item(key = "new-word") {
                        TextField(
                            value = newWord.orEmpty(),
                            onValueChange = { newWord = it },
                            label = L.str(context, "New word"),
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = null,
                                    tint = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { commitNewWord() }),
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                        )
                    }
                }

                items(words, key = { it.word }) { word ->
                    SwipeToDeleteWordRow(
                        word = word,
                        onDelete = { storage.remove(word) },
                    )
                }
            }
        }
    }
}

/** Swipe-to-delete wrapper with a destructive red background. */
@Composable
private fun SwipeToDeleteWordRow(word: BlockWord, onDelete: () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    // Miuix 0.8.8 has no SwipeToDismiss equivalent; SwipeToDismissBox itself
    // stays material3, only its colors moved to the Miuix color scheme.
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Row(
                Modifier.fillMaxWidth()
                    .background(MiuixTheme.colorScheme.errorContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = null,
                    tint = MiuixTheme.colorScheme.onErrorContainer,
                )
            }
        },
    ) {
        WordRow(word)
    }
}

/** A stored word: user words render as chips, plain words as text rows. */
@Composable
private fun WordRow(word: BlockWord) {
    val userName = word.word.takeIf { it.startsWith(BlockWordsStorage.userPrefix) }
        ?.removePrefix(BlockWordsStorage.userPrefix)
    Surface(
        color = MiuixTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (userName != null) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = MiuixTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(50),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(
                            Icons.Filled.AccountCircle,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            userName,
                            style = MiuixTheme.textStyles.body2,
                            color = MiuixTheme.colorScheme.onSurface,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        } else {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(word.word, style = MiuixTheme.textStyles.body1)
            }
        }
    }
}
