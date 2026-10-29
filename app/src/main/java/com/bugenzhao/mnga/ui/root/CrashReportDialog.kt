package com.bugenzhao.mnga.ui.root

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.bugenzhao.mnga.util.CrashReporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.WindowDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Diagnostic build (v1.7.11): if the previous run died with an uncaught
 * exception, [CrashReporter] stored its stack — show it here, in full,
 * with a copy button so it can be sent back for a targeted fix. Dismissing
 * (button or outside) marks the record as seen so it is not shown again.
 */
@Composable
fun CrashReportDialog() {
    val context = LocalContext.current
    var pending by remember { mutableStateOf<CrashReporter.PendingCrash?>(null) }
    LaunchedEffect(Unit) {
        pending = withContext(Dispatchers.IO) { CrashReporter.pendingCrash(context) }
    }
    val crash = pending ?: return

    fun dismiss() {
        CrashReporter.markSeen(context, crash.timeMillis)
        pending = null
    }

    WindowDialog(
        show = true,
        title = "上次崩溃记录（诊断版）",
        onDismissRequest = { dismiss() },
    ) {
        Column {
            Text(
                text = crash.text,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                style = MiuixTheme.textStyles.footnote1.copy(fontFamily = FontFamily.Monospace),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(
                    text = "复制堆栈",
                    onClick = {
                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(
                            ClipData.newPlainText("LumaGA crash", crash.text),
                        )
                        Toast.makeText(context, "已复制，请粘贴发回", Toast.LENGTH_SHORT).show()
                    },
                )
                Spacer(Modifier.size(8.dp))
                TextButton(
                    text = "关闭",
                    onClick = { dismiss() },
                )
            }
        }
    }
}
